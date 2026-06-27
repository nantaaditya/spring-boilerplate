package com.nantaaditya.example.strategy.retry;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;

@Setter
@Getter
public class UniformRandomBackOffPolicy implements BackOff {

  private static final long DEFAULT_MIN_BACKOFF_PERIOD = 1000;
  private static final long DEFAULT_MAX_BACKOFF_PERIOD = 5000;
  private static final int DEFAULT_MAX_ATTEMPTS = Integer.MAX_VALUE;

  private static String OBJECT_NAME = """
      UniformRandomBackOffPolicy[min period=%s, max period=%s, max attempts=%s]
      """;

  private long minPeriod = DEFAULT_MIN_BACKOFF_PERIOD;
  private long maxPeriod = DEFAULT_MAX_BACKOFF_PERIOD;
  private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

  public UniformRandomBackOffPolicy(long minPeriod, long maxPeriod, int maxAttempts) {
    if (minPeriod < 0 || maxPeriod < minPeriod || maxAttempts < 0) {
      throw new IllegalArgumentException("minPeriod must be >= 0, maxPeriod must be >= minPeriod, "
          + "and maxAttempts must be >= 0");
    }
    this.minPeriod = minPeriod;
    this.maxPeriod = maxPeriod;
    this.maxAttempts = maxAttempts;
  }

  @Override
  public String toString() {
    String attemptValue = (this.maxAttempts == Integer.MAX_VALUE ? "unlimited" : String.valueOf(this.maxAttempts));
    return String.format(OBJECT_NAME, minPeriod, maxPeriod, attemptValue);
  }

  @Override
  public BackOffExecution start() {
    return new UniformRandomBackOffExecution();
  }

  private class UniformRandomBackOffExecution implements BackOffExecution {

    private AtomicInteger currentAttempts = new AtomicInteger(0);

    private static String OBJECT_NAME = """
        UniformRandomBackOffExecution[min period=%s, max period=%s, current attempts=%s, max attempts=%s]
        """;

    @Override
    public long nextBackOff() {
      if (currentAttempts.get() < getMaxAttempts()) {
        currentAttempts.incrementAndGet();
        return ThreadLocalRandom.current().nextLong(minPeriod, maxPeriod + 1);
      }
      return STOP;
    }

    @Override
    public String toString() {
      String attemptValue = (maxAttempts == Integer.MAX_VALUE ? "unlimited" : String.valueOf(maxAttempts));
      return String.format(OBJECT_NAME, minPeriod, maxPeriod, currentAttempts.get(), attemptValue);
    }
  }
}
