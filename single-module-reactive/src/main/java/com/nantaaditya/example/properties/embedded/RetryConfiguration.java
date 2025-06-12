package com.nantaaditya.example.properties.embedded;

import com.nantaaditya.example.model.constant.RetryType;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record RetryConfiguration(
    RetryType type,
    long maxAttempt,
    int backoffTime,
    TimeUnit backoffTimeUnit,
    int fixedDelayTime,
    TimeUnit fixedDelayTimeUnit,
    String retryableExceptions
) {

  public Map<Class<? extends Throwable>, Boolean> getRetryableExceptions() {
    if (retryableExceptions == null) {
      return new HashMap<>();
    }

    StringTokenizer tokens = new StringTokenizer(retryableExceptions);
    Map<Class<? extends Throwable>, Boolean> maps = new HashMap<>();

    while (tokens.hasMoreTokens()) {
      String[] token = tokens.nextToken().split(":");
      try {
        maps.put((Class<? extends Throwable>) Class.forName(token[0]), Boolean.valueOf(token[1])); //NOSONAR
      } catch (ClassNotFoundException ex) {
        log.error("#Retry - error convert retryable exception {}", ex.getException().getMessage());
      }
    }
    return maps;
  }

}
