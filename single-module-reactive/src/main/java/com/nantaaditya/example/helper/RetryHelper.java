package com.nantaaditya.example.helper;

import com.nantaaditya.example.properties.RetryProperties;
import com.nantaaditya.example.properties.embedded.RetryConfiguration;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryHelper {

  private final RetryProperties retryProperties;
  private Map<String, Retry> retries = new ConcurrentHashMap<>();

  @EventListener(ApplicationReadyEvent.class)
  public void onStart() {
    if (retryProperties.configurations() == null) {
      return;
    }

    retryProperties.configurations()
      .forEach((key, value) -> {
        RetryBackoffSpec retry = createRetry(value);

        if (!value.getRetryableExceptions().isEmpty()) {
          retry = retry
              .filter(exception -> isRetryable(exception, value.getRetryableExceptions()))
              .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                log.error("#Retry - exhausted retry {} error {}", key, retrySignal.failure().getMessage());
                throw new IllegalStateException("#Retry - exhausted retry " + key, retrySignal.failure());
              });
        }

        retries.put(key, retry);
      }
    );
  }

  public Retry getRetry(String key) {
    Retry retry = retries.get(key);
    if (retry == null) {
      throw new IllegalStateException("#Retry - no retry " + key);
    }
    return retry;
  }

  private RetryBackoffSpec createRetry(RetryConfiguration configuration) {
    return switch (configuration.type()) {
      case BACKOFF -> Retry.backoff(
        configuration.maxAttempt(),
        Duration.of(
          configuration.backoffTime(),
          toTemporalUnit(configuration.backoffTimeUnit())
        )
      );
      case FIXED_DELAY -> Retry.fixedDelay(
        configuration.maxAttempt(),
        Duration.of(
          configuration.fixedDelayTime(),
          toTemporalUnit(configuration.fixedDelayTimeUnit())
        )
      );
    };
  }

private boolean isRetryable(Throwable throwable, Map<Class<? extends Throwable>, Boolean> retryableMap) {
  for (Map.Entry<Class<? extends Throwable>, Boolean> entry : retryableMap.entrySet()) {
    if (entry.getKey().isAssignableFrom(throwable.getClass())) {
      return entry.getValue();
    }
  }
  return false;
}

  private TemporalUnit toTemporalUnit(TimeUnit timeUnit) {
    return switch (timeUnit) {
      case NANOSECONDS -> ChronoUnit.NANOS;
      case MICROSECONDS -> ChronoUnit.MICROS;
      case MILLISECONDS -> ChronoUnit.MILLIS;
      case SECONDS -> ChronoUnit.SECONDS;
      case MINUTES -> ChronoUnit.MINUTES;
      case HOURS -> ChronoUnit.HOURS;
      case DAYS -> ChronoUnit.DAYS;
    };
  }
}
