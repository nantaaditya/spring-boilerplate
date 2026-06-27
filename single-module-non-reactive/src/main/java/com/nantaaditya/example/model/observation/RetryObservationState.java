package com.nantaaditya.example.model.observation;

import io.micrometer.observation.Observation;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-execution carrier for an in-flight retry observation. Owned by ObservableRetryable — lifetime
 * equals the RetryTemplate.invoke() call. AtomicInteger for attempts pre-empts any future async
 * retry engine.
 */
public final class RetryObservationState {

  private final Observation observation;
  private final RetryObservationContext context;
  private final AtomicInteger attempts = new AtomicInteger(0);

  public RetryObservationState(Observation observation, RetryObservationContext context) {
    this.observation = observation;
    this.context = context;
  }

  public Observation observation() {
    return observation;
  }

  public RetryObservationContext context() {
    return context;
  }

  public int attempts() {
    return attempts.get();
  }

  public void incrementAttempts() {
    attempts.incrementAndGet();
  }
}
