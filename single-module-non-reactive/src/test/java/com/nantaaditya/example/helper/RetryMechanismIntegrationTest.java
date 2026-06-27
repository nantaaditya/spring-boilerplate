package com.nantaaditya.example.helper;

import static io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nantaaditya.example.api.BaseIntegrationTest;
import com.nantaaditya.example.listener.RetryTemplateListener;
import com.nantaaditya.example.model.constant.BackoffPolicyConstant;
import com.nantaaditya.example.properties.embedded.RetryConfiguration;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.databind.ObjectMapper;

class RetryMechanismIntegrationTest extends BaseIntegrationTest {

  private static final String TEMPLATE_NAME = "test-retry";
  private static final long INTERVAL_MS = 50L;

  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private DeadLetterProcessRepository deadLetterProcessRepository;
  @Autowired
  private RetryHelper retryHelper;

  private TestObservationRegistry observationRegistry;
  private SimpleMeterRegistry meterRegistry;

  @Override
  protected String getClientId() {
    return "retry-integration";
  }

  @BeforeEach
  void setUp() {
    observationRegistry = TestObservationRegistry.create();
    meterRegistry = new SimpleMeterRegistry();
    observationRegistry.observationConfig()
        .observationHandler(new DefaultMeterObservationHandler(meterRegistry));
  }

  @Test
  @DisplayName("exhausted: action called 4 times, outcome=exhausted, attempts=3, timer>=100ms")
  void exhausted_assertRetryCountObservationAndDuration() {
    AtomicInteger counter = new AtomicInteger(0);
    Function<String, String> action = req -> {
      counter.incrementAndGet();
      throw new IllegalArgumentException("error");
    };

    RetryConfiguration config = config(3, "java.lang.IllegalArgumentException:true");
    RetryTemplate template = buildTemplate(config);

    assertThrows(IllegalArgumentException.class,
        () -> retryHelper.execute(template, config.maxAttempt(), "type", "name", action, null,
            "req"));

    assertThat(counter.get()).isEqualTo(4);

    assertThat(observationRegistry)
        .hasObservationWithNameEqualTo("app.retry").that()
        .hasLowCardinalityKeyValue("retry_name", TEMPLATE_NAME)
        .hasLowCardinalityKeyValue("outcome", "exhausted")
        .hasLowCardinalityKeyValue("attempts", "3")
        .hasBeenStopped();

    assertThat(attemptCounterTotal()).isEqualTo(3.0);
    // Observation spans from onRetryFailure(retry 1) to onRetryPolicyExhaustion = 2 backoff sleeps
    assertThat(timerTotalMs()).isGreaterThanOrEqualTo(2 * INTERVAL_MS);
  }

  @Test
  @DisplayName("success after 2 failures: action called 3 times, outcome=success, attempts=1 (initial not counted), timer>=50ms")
  void successAfterFailures_assertRetryCountObservationAndDuration() {
    AtomicInteger counter = new AtomicInteger(0);
    Function<String, String> action = req -> {
      if (counter.incrementAndGet() <= 2) {
        throw new IllegalArgumentException("not yet");
      }
      return "done";
    };

    RetryConfiguration config = config(3, "java.lang.IllegalArgumentException:true");
    RetryTemplate template = buildTemplate(config);

    String result = retryHelper.execute(template, config.maxAttempt(), "type", "name", action, null,
        "req");

    assertThat(result).isEqualTo("done");
    assertThat(counter.get()).isEqualTo(3);

    // Spring 7's RetryTemplate only fires onRetryFailure/onRetrySuccess for RETRY attempts.
    // Initial attempt is not counted. With fail-fail-succeed: retry 1 fails (attempts=1),
    // retry 2 succeeds (onRetrySuccess stops observation).
    assertThat(observationRegistry)
        .hasObservationWithNameEqualTo("app.retry").that()
        .hasLowCardinalityKeyValue("retry_name", TEMPLATE_NAME)
        .hasLowCardinalityKeyValue("outcome", "success")
        .hasLowCardinalityKeyValue("attempts", "1")
        .hasBeenStopped();

    assertThat(attemptCounterTotal()).isEqualTo(1.0);
    assertThat(timerTotalMs()).isGreaterThanOrEqualTo(INTERVAL_MS);
  }

  @Test
  @DisplayName("fallback: action called once, no retry triggered, no observation, no attempt counter")
  void fallback_assertNoRetryAndSuccessObservation() {
    AtomicInteger counter = new AtomicInteger(0);
    Function<String, String> action = req -> {
      counter.incrementAndGet();
      throw new IllegalArgumentException("error");
    };
    Function<IllegalArgumentException, String> fallback = ex -> "fallback-result";

    RetryConfiguration config = config(3, "java.lang.IllegalArgumentException:true");
    RetryTemplate template = buildTemplate(config);

    String result = retryHelper.execute(template, config.maxAttempt(), "type", "name", action,
        fallback, "req");

    assertThat(result).isEqualTo("fallback-result");
    assertThat(counter.get()).isEqualTo(1);

    // Spring 7's RetryTemplate does not call onRetrySuccess for first-attempt success.
    // Fallback is handled inside the Retryable itself (no exception propagates to the template),
    // so the initial attempt "succeeds" and no listener callbacks fire.
    assertThat(observationRegistry).hasNumberOfObservationsEqualTo(0);
    assertThat(meterRegistry.find("app.retry.attempts").counter()).isNull();
  }

  @Test
  @DisplayName("feature tag: processName=default resolves to RetryFeatureConstant.DEFAULT")
  void featureTag_assertTaggedFromProcessName() {
    Function<String, String> action = req -> {
      throw new IllegalArgumentException("error");
    };

    RetryConfiguration config = config(1, "java.lang.IllegalArgumentException:true");
    RetryTemplate template = buildTemplate(config);

    assertThrows(IllegalArgumentException.class,
        () -> retryHelper.execute(template, config.maxAttempt(), "type", "default", action, null,
            "req"));

    assertThat(observationRegistry)
        .hasObservationWithNameEqualTo("app.retry").that()
        .hasLowCardinalityKeyValue("retry_name", TEMPLATE_NAME)
        .hasLowCardinalityKeyValue("feature", "default")
        .hasBeenStopped();
  }

  // --- helpers ---

  private RetryConfiguration config(int maxAttempt, String retryableExceptions) {
    return new RetryConfiguration(
        BackoffPolicyConstant.FIXED, INTERVAL_MS, 1.0, 5000, maxAttempt, retryableExceptions);
  }

  private RetryTemplate buildTemplate(RetryConfiguration config) {
    RetryPolicy.Builder builder = RetryPolicy.builder()
        .backOff(new FixedBackOff(config.initialInterval(), config.maxAttempt()));

    List<Class<? extends Throwable>> includes = config.getWhitelistedExceptions();
    if (!includes.isEmpty()) {
      builder.includes(includes);
    }
    List<Class<? extends Throwable>> excludes = config.getBlacklistedExceptions();
    if (!excludes.isEmpty()) {
      builder.excludes(excludes);
    }

    RetryTemplate template = new RetryTemplate();
    template.setRetryPolicy(builder.build());
    template.setRetryListener(new RetryTemplateListener(TEMPLATE_NAME, objectMapper,
        deadLetterProcessRepository, observationRegistry, meterRegistry));
    return template;
  }

  private double attemptCounterTotal() {
    return meterRegistry.find("app.retry.attempts").counters().stream()
        .mapToDouble(c -> c.count())
        .sum();
  }

  private double timerTotalMs() {
    return meterRegistry.find("app.retry").timers().stream()
        .mapToDouble(t -> t.totalTime(TimeUnit.MILLISECONDS))
        .sum();
  }
}
