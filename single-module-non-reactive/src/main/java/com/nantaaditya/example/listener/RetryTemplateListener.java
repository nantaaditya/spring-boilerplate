package com.nantaaditya.example.listener;

import com.nantaaditya.example.entity.DeadLetterProcess;
import com.nantaaditya.example.helper.ContextHelper;
import com.nantaaditya.example.helper.RetryExhaustionNotifier;
import com.nantaaditya.example.model.constant.AppsFeatureConstant;
import com.nantaaditya.example.model.constant.RetryConstant;
import com.nantaaditya.example.model.constant.RetryFeatureConstant;
import com.nantaaditya.example.model.constant.RetryMetricTag;
import com.nantaaditya.example.model.constant.RetryResult;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.model.dto.ContextDTO;
import com.nantaaditya.example.model.dto.RetryHistoryContext;
import com.nantaaditya.example.model.observation.ObservableRetryable;
import com.nantaaditya.example.model.observation.RetryObservationContext;
import com.nantaaditya.example.model.observation.RetryObservationConvention;
import com.nantaaditya.example.model.observation.RetryObservationState;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import com.nantaaditya.example.spi.RetryExhaustionSource;
import com.nantaaditya.example.spi.RetryOutcomeEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.Nullable;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.Retryable;
import tools.jackson.databind.ObjectMapper;

@Log4j2
public class RetryTemplateListener implements RetryListener {

  private static final String ATTEMPTS_METRIC  = "app.retry.attempts";

  private final String name;
  private final ObjectMapper objectMapper;
  private final DeadLetterProcessRepository deadLetterProcessRepository;
  private final ObservationRegistry observationRegistry;
  private final MeterRegistry meterRegistry;
  private final RetryExhaustionNotifier retryExhaustionNotifier;
  private final RetryObservationConvention convention = new RetryObservationConvention();

  public RetryTemplateListener(String name, ObjectMapper objectMapper,
      DeadLetterProcessRepository deadLetterProcessRepository,
      ObservationRegistry observationRegistry, MeterRegistry meterRegistry,
      RetryExhaustionNotifier retryExhaustionNotifier) {
    this.name = name;
    this.objectMapper = objectMapper;
    this.deadLetterProcessRepository = deadLetterProcessRepository;
    this.observationRegistry = observationRegistry;
    this.meterRegistry = meterRegistry;
    this.retryExhaustionNotifier = retryExhaustionNotifier;
  }

  @Override
  public void onRetrySuccess(RetryPolicy retryPolicy, Retryable<?> retryable,
      @Nullable Object result) {
    log.debug(AppLogMessage.message("#RETRY - retry success {} {}", name, retryable.getName()));
    if (retryable instanceof ObservableRetryable<?> wrapper && wrapper.state() != null) {
      stopObservation(wrapper.state(), RetryResult.SUCCESS, "none");
    }
  }

  @Override
  public void onRetryFailure(RetryPolicy retryPolicy, Retryable<?> retryable, Throwable throwable) {
    if (retryable instanceof ObservableRetryable<?> wrapper) {
      Map<String, Object> retryContext = wrapper.retryContext();
      log.error(AppLogMessage.message("#RETRY - error retry {} {} ", name,
          safeContextLog(retryContext)).error(throwable));
      RetryObservationState state = ensureState(wrapper, retryContext);
      state.incrementAttempts();
      meterRegistry.counter(ATTEMPTS_METRIC,
          RetryMetricTag.RETRY_NAME.getTag(), safe(name),
          RetryMetricTag.FEATURE.getTag(),    state.context().getFeature(),
          RetryMetricTag.EXCEPTION.getTag(),  simpleName(rootCause(throwable))
      ).increment();
    } else {
      log.error(AppLogMessage.message("#RETRY - error retry {} {}", name,
          throwable.getMessage()).error(throwable));
    }
  }

  @Override
  public void onRetryPolicyExhaustion(RetryPolicy retryPolicy, Retryable<?> retryable,
      RetryException exception) {
    Map<String, Object> retryContext = (retryable instanceof ObservableRetryable<?> w)
        ? w.retryContext() : null;
    Throwable cause = exception.getCause();

    if (retryContext != null) {
      log.error(AppLogMessage.message("#RETRY - close retry {} {}", name,
          safeContextLog(retryContext)).error(cause));
      saveExhaustedRetry(exception, retryContext);
    } else {
      log.error(AppLogMessage.message("#RETRY - close retry {} {}", name,
          cause == null ? RetryResult.UNKNOWN : cause.getMessage()).error(cause));
    }

    if (retryable instanceof ObservableRetryable<?> wrapper) {
      RetryObservationState state = ensureState(wrapper, retryContext);
      stopObservation(state, RetryResult.EXHAUSTED, simpleName(rootCause(exception)));
    }
  }

  @Override
  public void onRetryPolicyTimeout(RetryPolicy retryPolicy, Retryable<?> retryable,
      RetryException exception) {
    if (retryable instanceof ObservableRetryable<?> wrapper) {
      RetryObservationState state = ensureState(wrapper, wrapper.retryContext());
      stopObservation(state, RetryResult.TIMEOUT, simpleName(rootCause(exception)));
    }
  }

  @Override
  public void onRetryPolicyInterruption(RetryPolicy retryPolicy, Retryable<?> retryable,
      RetryException exception) {
    if (retryable instanceof ObservableRetryable<?> wrapper) {
      RetryObservationState state = ensureState(wrapper, wrapper.retryContext());
      stopObservation(state, RetryResult.INTERRUPTED, simpleName(rootCause(exception)));
    }
  }

  // --- observation lifecycle ---
  private RetryObservationState ensureState(ObservableRetryable<?> wrapper,
      @Nullable Map<String, Object> retryContext) {
    if (wrapper.state() != null) {
      return wrapper.state();
    }

    RetryObservationContext context = new RetryObservationContext(name);
    context.setFeature(getFeature(retryContext));
    context.setRequestId(getRequestId(retryContext));

    Observation observation = Observation
        .createNotStarted(RetryObservationConvention.OBSERVATION_NAME, () -> context, observationRegistry)
        .observationConvention(convention)
        .start();

    RetryObservationState state = new RetryObservationState(observation, context);
    wrapper.initState(state);
    return state;
  }

  private void stopObservation(RetryObservationState state, RetryResult outcome, String exception) {
    try {
      RetryObservationContext context = state.context();
      context.setOutcome(outcome.name().toLowerCase());
      context.setException(exception);
      context.setAttempts(state.attempts());
      state.observation().stop();
    } catch (Exception e) {
      log.warn(AppLogMessage.message("#RETRY - failed to stop observation {} {}", name,
          e.getMessage()).error(e));
    }
  }

  // --- context resolution ---
  private String getFeature(@Nullable Map<String, Object> retryContext) {
    if (retryContext != null) {
      Object processName = retryContext.get(RetryConstant.PROCESS_NAME.getName());
      if (processName instanceof String s && !s.isBlank()) {
        return RetryFeatureConstant.getByName(s).getName();
      }
    }

    ContextDTO ctx = ContextHelper.get();
    if (ctx != null && ctx.method() != null && ctx.path() != null) {
      AppsFeatureConstant feature = AppsFeatureConstant.get(ctx.method(), ctx.path());
      return Optional.ofNullable(feature)
          .map(AppsFeatureConstant::name)
          .orElse(ctx.getUnknownFeature());
    }
    return RetryResult.UNKNOWN.name();
  }

  private String getRequestId(@Nullable Map<String, Object> retryContext) {
    if (retryContext != null) {
      Object requestId = retryContext.get(RetryConstant.REQUEST_ID.getName());
      if (requestId instanceof String s && !s.isBlank()) {
        return s;
      }
    }
    ContextDTO ctx = ContextHelper.get();
    return Optional.ofNullable(ctx)
        .map(ContextDTO::requestId)
        .orElse("");
  }

  // --- dead-letter persistence (behaviour unchanged) ---
  private void saveExhaustedRetry(RetryException retryException, Map<String, Object> retryContext) {
    Throwable throwable = retryException.getLastException();
    if (throwable == null) return;

    try {
      log.error(AppLogMessage.message("#RETRY - last error {}", throwable.getMessage()).error(throwable));
      byte[] request = objectMapper.writeValueAsBytes(retryContext.get(RetryConstant.REQUEST.getName()));

      List<RetryHistoryContext> retryHistories = new LinkedList<>();
      retryHistories.add(new RetryHistoryContext(
          0,
          (String) retryContext.get(RetryConstant.RESPONSE.getName()),
          retryException.getLastException().getMessage()
      ));
      byte[] retryHistoriesBytes = objectMapper.writeValueAsBytes(retryHistories);

      deadLetterProcessRepository.save(
          DeadLetterProcess.create(retryContext, request, retryHistoriesBytes, throwable));

      int maxRetry;
      if (retryContext.get(RetryConstant.MAX_RETRY.getName()) instanceof Integer i) {
        maxRetry = i;
      } else {
        maxRetry = 0;
        log.warn(AppLogMessage.message("#RETRY - missing/invalid max_retry in context, "
            + "reporting exhaustion event with maxRetry=0 {}", safeContextLog(retryContext)));
      }
      retryExhaustionNotifier.notifyExhausted(new RetryOutcomeEvent(
          (String) retryContext.get(RetryConstant.PROCESS_TYPE.getName()),
          (String) retryContext.get(RetryConstant.PROCESS_NAME.getName()),
          (String) retryContext.get(RetryConstant.REQUEST_ID.getName()),
          maxRetry,
          maxRetry,
          throwable.getMessage(),
          RetryExhaustionSource.INITIAL_RETRY,
          LocalDateTime.now()));
    } catch (Exception e) {
      log.error(AppLogMessage.message("#RETRY - failed to save exhausted retry {} {}",
          safeContextLog(retryContext), e.getMessage()).error(e));
    }
  }

  // --- helpers ---
  private Map<String, Object> safeContextLog(Map<String, Object> retryContext) {
    return retryContext.entrySet().stream()
        .filter(entry -> !entry.getKey().equals(RetryConstant.EXCEPTION.getName()))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  private static Throwable rootCause(Throwable throwable) {
    if (throwable == null) return null;
    Throwable current = throwable;
    while (current.getCause() != null && current.getCause() != current) {
      current = current.getCause();
    }
    return current;
  }

  private static String simpleName(Throwable throwable) {
    return Optional.ofNullable(throwable)
        .map(Throwable::getClass)
        .map(Class::getSimpleName)
        .orElse("");
  }

  private static String safe(String value) {
    return Optional.ofNullable(value).orElse("");
  }
}
