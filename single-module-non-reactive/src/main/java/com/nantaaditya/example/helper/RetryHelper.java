package com.nantaaditya.example.helper;

import com.nantaaditya.example.model.constant.RetryConstant;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.model.observation.ObservableRetryable;
import com.nantaaditya.example.properties.RetryProperties;
import com.nantaaditya.example.properties.embedded.RetryConfiguration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class RetryHelper {

  private final RetryTemplateHelper retryTemplateHelper;
  private final RetryProperties retryProperties;

  private static final String RETRY_TEMPLATE_POSTFIX = "RetryTemplate";

  @SneakyThrows
  public <S, T, E extends Throwable> T execute(String retryName, String processType,
      String processName, Function<S, T> action, Function<E, T> fallbackAction, S request) {
    RetryTemplate retryTemplate = retryTemplateHelper.getRetryTemplate(retryName);
    int maxRetry = Optional.ofNullable(retryProperties.get(retryName))
        .map(RetryConfiguration::maxAttempt)
        .orElse(1);
    return execute(retryTemplate, maxRetry, processType, processName, action, fallbackAction, request);
  }

  @SneakyThrows
  public <S, T, E extends Throwable> T execute(RetryTemplate retryTemplate, int maxRetry,
      String processType, String processName, Function<S, T> action, Function<E, T> fallbackAction, S request) {
    Map<String, Object> retryContext = new ConcurrentHashMap<>();
    ObservableRetryable<T> wrapper = new ObservableRetryable<>(
        () -> execute(processType, processName, action, fallbackAction, request, retryContext, maxRetry),
        retryContext
    );
    try {
      return retryTemplate.execute(wrapper);
    } catch (RetryException e) {
      throw e.getCause() != null ? e.getCause() : e;
    }
  }

  @SneakyThrows
  private <S, T, E extends Throwable> T execute(String processType, String processName,
      Function<S, T> action, Function<E, T> fallbackAction, S request,
      Map<String, Object> context, int maxRetry) {
    T response = null;
    try {
      log.debug(AppLogMessage.message("#Retry - execute request [{}] [{}] - {}",
          processType, processName, request));
      response = action.apply(request);
      return response;
    } catch (Throwable ex) {
      if (fallbackAction != null) {
        try {
          @SuppressWarnings("unchecked")
          E typed = (E) ex;
          return fallbackAction.apply(typed);
        } catch (ClassCastException cce) {
          throw new IllegalStateException(
              "Fallback type mismatch: expected compatible type but got " + ex.getClass().getName(), cce);
        }
      }
      updateRetryContext(processType, processName, context, request, response, ex, maxRetry);
      throw ex;
    }
  }

  private static <S, T> void updateRetryContext(String processType, String processName,
      Map<String, Object> context, S request, T response, Throwable e, int maxRetry) {
    String requestId = ContextHelper.getRequestId();
    if (requestId != null) {
      context.put(RetryConstant.REQUEST_ID.getName(), requestId);
    }
    context.put(RetryConstant.REQUEST.getName(), request);
    context.put(RetryConstant.EXCEPTION.getName(), Optional.ofNullable(e).map(Throwable::getCause).orElse(e));
    context.put(RetryConstant.PROCESS_TYPE.getName(), processType);
    context.put(RetryConstant.PROCESS_NAME.getName(), processName);
    context.put(RetryConstant.MAX_RETRY.getName(), maxRetry);
    if (response != null) {
      context.put(RetryConstant.RESPONSE.getName(), response);
    }
  }
}
