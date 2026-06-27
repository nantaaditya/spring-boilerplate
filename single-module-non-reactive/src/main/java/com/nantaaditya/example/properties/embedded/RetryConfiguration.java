package com.nantaaditya.example.properties.embedded;

import com.nantaaditya.example.model.constant.BackoffPolicyConstant;
import com.nantaaditya.example.model.dto.AppLogMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import lombok.extern.log4j.Log4j2;

@Log4j2
public record RetryConfiguration(
    BackoffPolicyConstant type,
    long initialInterval,
    double multiplier,
    long maxInterval,
    int maxAttempt,
    String retryableExceptions
) {

  public List<Class<? extends Throwable>> getWhitelistedExceptions() {
    return getRetryableExceptionList(true);
  }

  public List<Class<? extends Throwable>> getBlacklistedExceptions() {
    return getRetryableExceptionList(false);
  }

  private List<Class<? extends Throwable>> getRetryableExceptionList(boolean isRetryable) {
    if (retryableExceptions == null) {
      return Collections.emptyList();
    }

    StringTokenizer tokens = new StringTokenizer(retryableExceptions, ",");
    List<Class<? extends Throwable>> list = new ArrayList<>();
    while (tokens.hasMoreTokens()) {
      String[] token = tokens.nextToken().split(":");

      if (token == null || token.length != 2) {
        continue;
      }

      try {
        Class<?> clazz = Class.forName(token[0]);
        if (!Throwable.class.isAssignableFrom(clazz)) {
          log.info(AppLogMessage.message("#Retry - class not extends Throwable, skipping: {}", clazz));
          continue;
        }

        if (Boolean.parseBoolean(token[1]) == isRetryable) {
          Class<? extends Throwable> throwableClass = (Class<? extends Throwable>) clazz; //NOSONAR
          list.add(throwableClass); //NOSONAR
        }
      } catch (ClassNotFoundException ex) {
        log.error(AppLogMessage.message("#Retry - could not load retry exception map {}", ex.getMessage())
            .error(ex));
      }
    }
    return list;
  }
}
