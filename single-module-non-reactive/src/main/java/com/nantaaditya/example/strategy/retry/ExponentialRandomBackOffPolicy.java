package com.nantaaditya.example.strategy.retry;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;

@Setter
@Getter
public class ExponentialRandomBackOffPolicy implements BackOff {

  private static final long DEFAULT_INITIAL_INTERVAL = 1000;
  private static final double DEFAULT_MULTIPLIER = 1.0d;
  private static final long DEFAULT_MAX_INTERVAL = Long.MAX_VALUE;

  private static String OBJECT_NAME = """
      ExponentialRandomBackOffPolicy[min interval=%s, max interval=%s, multiplier=%s, max attempts=%s]
      """;

  private long initialInterval = DEFAULT_INITIAL_INTERVAL;
  private double multiplier = DEFAULT_MULTIPLIER;
  private long maxInterval = DEFAULT_MAX_INTERVAL;
  private int maxAttempts = Integer.MAX_VALUE;

  public ExponentialRandomBackOffPolicy(long initialInterval, double multiplier, long maxInterval, int maxAttempts) {
    this.initialInterval = initialInterval;
    this.multiplier = multiplier;
    this.maxInterval = maxInterval;
    this.maxAttempts = maxAttempts;
  }

  @Override
  public String toString() {
    String attemptValue = (this.maxAttempts == Integer.MAX_VALUE ? "unlimited" : String.valueOf(this.maxAttempts));
    return String.format(OBJECT_NAME, initialInterval, maxInterval, multiplier, attemptValue);
  }

  @Override
  public BackOffExecution start() {
    return new ExponentialRandomBackOffExecution();
  }

  private class ExponentialRandomBackOffExecution implements BackOffExecution {

    private AtomicInteger currentAttempts = new AtomicInteger(0);

    private static String OBJECT_NAME = """
        ExponentialRandomBackOff[min interval=%s, max interval=%s, multiplier=%s, current attempts=%s, max attempts=%s]
        """;

    @Override
    public long nextBackOff() {
      if (currentAttempts.get() >= getMaxAttempts()) {
        return STOP;
      }

      long exponentialCeiling = (long) (initialInterval * Math.pow(multiplier, currentAttempts.get()));
      if (exponentialCeiling > maxInterval || exponentialCeiling <= 0) {
        return STOP;
      }

      long cappedCeiling = Math.min(maxInterval, exponentialCeiling);
      long delay = ThreadLocalRandom.current().nextLong(cappedCeiling + 1);
      currentAttempts.incrementAndGet();
      return delay;
    }

    @Override
    public String toString() {
      String attemptValue = (maxAttempts == Integer.MAX_VALUE ? "unlimited" : String.valueOf(maxAttempts));
      return String.format(OBJECT_NAME, initialInterval, maxInterval, multiplier, currentAttempts.get(), attemptValue);
    }
  }
}
