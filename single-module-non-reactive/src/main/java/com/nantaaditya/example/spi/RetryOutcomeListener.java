package com.nantaaditya.example.spi;

/**
 * Implement and register as a Spring bean to be notified of terminal retry outcomes.
 * Both methods default to no-op so implementers only override what they need.
 */
public interface RetryOutcomeListener {

  /**
   * Fired when a retry sequence is exhausted, whether from the initial/live retry
   * ({@link RetryExhaustionSource#INITIAL_RETRY}) or a dead-letter replay attempt
   * ({@link RetryExhaustionSource#DEAD_LETTER_REPLAY}).
   */
  default void onRetryExhausted(RetryOutcomeEvent event) {
  }

  /**
   * Fired when a dead-letter replay attempt succeeds. Not fired for ordinary
   * first-attempt success.
   */
  default void onReplaySuccess(RetryOutcomeEvent event) {
  }
}
