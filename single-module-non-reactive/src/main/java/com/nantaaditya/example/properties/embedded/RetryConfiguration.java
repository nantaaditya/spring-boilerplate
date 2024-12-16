package com.nantaaditya.example.properties.embedded;

import com.nantaaditya.example.helper.StringHelper;
import com.nantaaditya.example.model.constant.BackoffPolicyConstant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record RetryConfiguration(
    BackoffPolicyConstant type,
    long initialInterval,
    double multiplier,
    long maxInterval,
    int maxAttempt,
    String retryableExceptions
) {

  public Map<Class<? extends Throwable>, Boolean> getRetryableExceptionMaps() {
    if (retryableExceptions == null) {
      return new HashMap<>();
    }

    Map<Class<? extends Throwable>, Boolean> maps = new HashMap<>();
    Collection<String> tokens = StringHelper.toCollection(retryableExceptions, ",", HashSet.class);

    for (String token : tokens) {
      List<String> tuple = (List<String>) StringHelper.toCollection(token, ":", ArrayList.class);
      try {
        Class<?> clazz = Class.forName(tuple.getFirst());
        if (!Throwable.class.isAssignableFrom(clazz)) {
          log.info("#Retry - class not extends Throwable, skipping: {}", clazz);
          continue;
        }

        Class<? extends Throwable> throwableClass = (Class<? extends Throwable>) clazz; //NOSONAR
        maps.put(throwableClass, Boolean.valueOf(tuple.getLast())); //NOSONAR
      } catch (ClassNotFoundException ex) {
        log.error("#Retry - could not load retry exception map", ex);
      }
    }
    return maps;
  }
}
