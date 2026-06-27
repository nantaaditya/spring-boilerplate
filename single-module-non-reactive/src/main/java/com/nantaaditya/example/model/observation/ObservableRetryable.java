package com.nantaaditya.example.model.observation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.retry.Retryable;

public final class ObservableRetryable<T> implements Retryable<T> {

  private final Retryable<T> delegate;
  private RetryObservationState state;
  private final Map<String, Object> retryContext;

  public ObservableRetryable(Retryable<T> delegate) {
    this(delegate, new ConcurrentHashMap<>());
  }

  public ObservableRetryable(Retryable<T> delegate, Map<String, Object> retryContext) {
    this.delegate = delegate;
    this.retryContext = retryContext;
  }

  @Override
  public T execute() throws Throwable {
    return delegate.execute();
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  public RetryObservationState state() {
    return state;
  }

  public void initState(RetryObservationState state) {
    this.state = state;
  }

  public Map<String, Object> retryContext() {
    return retryContext;
  }
}