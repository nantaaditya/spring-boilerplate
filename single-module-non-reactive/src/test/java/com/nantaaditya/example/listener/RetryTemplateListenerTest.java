package com.nantaaditya.example.listener;

import static io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import com.nantaaditya.example.helper.RetryExhaustionNotifier;
import com.nantaaditya.example.model.constant.RetryConstant;
import com.nantaaditya.example.model.observation.ObservableRetryable;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import com.nantaaditya.example.spi.RetryExhaustionSource;
import com.nantaaditya.example.spi.RetryOutcomeEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.Retryable;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class RetryTemplateListenerTest {

  private static final String NAME = "client-a";

  @Mock
  private DeadLetterProcessRepository deadLetterProcessRepository;
  @Mock
  private RetryPolicy retryPolicy;
  @Mock
  private RetryExhaustionNotifier retryExhaustionNotifier;

  private TestObservationRegistry observationRegistry;
  private SimpleMeterRegistry meterRegistry;
  private RetryTemplateListener listener;

  @BeforeEach
  void setUp() {
    observationRegistry = TestObservationRegistry.create();
    meterRegistry = new SimpleMeterRegistry();
    listener = new RetryTemplateListener(NAME, JsonMapper.builder().build(),
        deadLetterProcessRepository, observationRegistry, meterRegistry, retryExhaustionNotifier);
  }

  private ObservableRetryable<String> wrapper() {
    return new ObservableRetryable<>(() -> "result");
  }

  @Test
  @DisplayName("first-try success: no observation recorded — only retry attempts are tracked")
  void onRetrySuccess_firstTry_recordsNothing() {
    ObservableRetryable<String> w = wrapper();

    listener.onRetrySuccess(retryPolicy, w, "ok");

    TestObservationRegistryAssert.assertThat(observationRegistry)
        .hasNumberOfObservationsEqualTo(0);
  }

  @Test
  @DisplayName("each failure increments app.retry.attempts counter with exception tag")
  void onRetryFailure_incrementsAttemptCounter() {
    ObservableRetryable<String> w = wrapper();
    RuntimeException ex = new RuntimeException(new IllegalArgumentException("boom"));

    listener.onRetryFailure(retryPolicy, w, ex);
    listener.onRetryFailure(retryPolicy, w, ex);

    double count = meterRegistry.get("app.retry.attempts")
        .tag("retry_name", NAME)
        .tag("exception", "IllegalArgumentException")
        .counter()
        .count();

    Assertions.assertThat(count).isEqualTo(2.0d);
  }

  @Test
  @DisplayName("failures then success: terminal observation carries attempt count")
  void failuresThenSuccess_attemptsTaggedCorrectly() {
    ObservableRetryable<String> w = wrapper();
    RuntimeException ex = new RuntimeException(new IllegalStateException("x"));

    listener.onRetryFailure(retryPolicy, w, ex);
    listener.onRetryFailure(retryPolicy, w, ex);
    listener.onRetrySuccess(retryPolicy, w, "ok");

    assertThat(observationRegistry)
        .hasObservationWithNameEqualTo("app.retry")
        .that()
        .hasLowCardinalityKeyValue("outcome", "success")
        .hasLowCardinalityKeyValue("attempts", "2")
        .hasBeenStopped();
  }

  @Test
  @DisplayName("exhaustion: observation stopped with outcome=exhausted")
  void onRetryPolicyExhaustion_recordsExhausted() {
    ObservableRetryable<String> w = wrapper();
    RetryException retryEx = new RetryException("exhausted",
        new IllegalArgumentException("cause"));

    listener.onRetryPolicyExhaustion(retryPolicy, w, retryEx);

    assertThat(observationRegistry)
        .hasObservationWithNameEqualTo("app.retry")
        .that()
        .hasLowCardinalityKeyValue("retry_name", NAME)
        .hasLowCardinalityKeyValue("outcome", "exhausted")
        .hasBeenStopped();
  }

  @Test
  @DisplayName("exhaustion: notifies RetryExhaustionNotifier with INITIAL_RETRY source")
  void onRetryPolicyExhaustion_notifiesExhaustionListener() {
    ObservableRetryable<String> w = wrapper();
    w.retryContext().put(RetryConstant.PROCESS_TYPE.getName(), "orderSync");
    w.retryContext().put(RetryConstant.PROCESS_NAME.getName(), "orderSyncName");
    w.retryContext().put(RetryConstant.REQUEST_ID.getName(), "req-123");
    w.retryContext().put(RetryConstant.MAX_RETRY.getName(), 3);
    RetryException retryEx = new RetryException("exhausted",
        new IllegalArgumentException("cause"));

    listener.onRetryPolicyExhaustion(retryPolicy, w, retryEx);

    verify(retryExhaustionNotifier).notifyExhausted(argThat((RetryOutcomeEvent event) ->
        "orderSync".equals(event.processType())
            && "orderSyncName".equals(event.processName())
            && "req-123".equals(event.idempotencyKey())
            && event.retryCount() == 3
            && event.maxRetry() == 3
            && event.source() == RetryExhaustionSource.INITIAL_RETRY));
  }

  @Test
  @DisplayName("plain Retryable bypassing RetryHelper degrades gracefully — no NPE")
  void plainRetryable_noObservableWrapper_degradesGracefully() {
    Retryable<String> plain = () -> "result";

    listener.onRetrySuccess(retryPolicy, plain, "ok");
    listener.onRetryFailure(retryPolicy, plain, new RuntimeException("x"));

    TestObservationRegistryAssert.assertThat(observationRegistry)
        .hasNumberOfObservationsEqualTo(0);
  }

  @Test
  @DisplayName("two independent executions each with a retry produce two independent observations")
  void twoExecutions_independentObservations() {
    ObservableRetryable<String> first = wrapper();
    ObservableRetryable<String> second = wrapper();
    RuntimeException ex = new RuntimeException("err");

    listener.onRetryFailure(retryPolicy, first, ex);
    listener.onRetrySuccess(retryPolicy, first, null);
    listener.onRetryFailure(retryPolicy, second, ex);
    listener.onRetrySuccess(retryPolicy, second, null);

    TestObservationRegistryAssert.assertThat(observationRegistry)
        .hasNumberOfObservationsEqualTo(2);
  }

  @Test
  @DisplayName("onRetryPolicyTimeout: observation stopped with outcome=timeout")
  void onRetryPolicyTimeout_stopsObservation() {
    ObservableRetryable<String> w = wrapper();
    RetryException retryEx = new RetryException("timeout", new IllegalArgumentException("cause"));

    listener.onRetryPolicyTimeout(retryPolicy, w, retryEx);

    assertThat(observationRegistry)
        .hasObservationWithNameEqualTo("app.retry")
        .that()
        .hasLowCardinalityKeyValue("retry_name", NAME)
        .hasLowCardinalityKeyValue("outcome", "timeout")
        .hasBeenStopped();
  }

  @Test
  @DisplayName("onRetryPolicyInterruption: observation stopped with outcome=interrupted")
  void onRetryPolicyInterruption_stopsObservation() {
    ObservableRetryable<String> w = wrapper();
    RetryException retryEx = new RetryException("interrupted", new IllegalArgumentException("cause"));

    listener.onRetryPolicyInterruption(retryPolicy, w, retryEx);

    assertThat(observationRegistry)
        .hasObservationWithNameEqualTo("app.retry")
        .that()
        .hasLowCardinalityKeyValue("retry_name", NAME)
        .hasLowCardinalityKeyValue("outcome", "interrupted")
        .hasBeenStopped();
  }

  @Test
  @DisplayName("getFeature: processName matching registered RetryFeatureConstant returns its name")
  void getFeature_knownProcessName_returnsEnumName() {
    ObservableRetryable<String> w = wrapper();
    w.retryContext().put(RetryConstant.PROCESS_NAME.getName(), "default");

    listener.onRetryFailure(retryPolicy, w, new RuntimeException("boom"));
    listener.onRetrySuccess(retryPolicy, w, "ok");

    assertThat(observationRegistry)
        .hasObservationWithNameEqualTo("app.retry")
        .that()
        .hasLowCardinalityKeyValue("feature", "default")
        .hasBeenStopped();
  }

  @Test
  @DisplayName("getFeature: unknown processName falls back to RetryFeatureConstant.DEFAULT")
  void getFeature_unknownProcessName_fallsBackToDefault() {
    ObservableRetryable<String> w = wrapper();
    w.retryContext().put(RetryConstant.PROCESS_NAME.getName(), "no-such-feature");

    listener.onRetryFailure(retryPolicy, w, new RuntimeException("boom"));
    listener.onRetrySuccess(retryPolicy, w, "ok");

    assertThat(observationRegistry)
        .hasObservationWithNameEqualTo("app.retry")
        .that()
        .hasLowCardinalityKeyValue("feature", "default")
        .hasBeenStopped();
  }
}
