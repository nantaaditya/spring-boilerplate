package com.nantaaditya.example.properties.embedded;

import com.nantaaditya.example.helper.StringHelper;
import com.nantaaditya.example.model.constant.BackoffPolicyConstant;
import com.nantaaditya.example.model.dto.AppLogMessage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
          log.info(AppLogMessage.create(String.format("#Retry - class not extends Throwable, skipping: %s", clazz)));
          continue;
        }

        Class<? extends Throwable> throwableClass = (Class<? extends Throwable>) clazz; //NOSONAR
        maps.put(throwableClass, Boolean.valueOf(tuple.getLast())); //NOSONAR
      } catch (ClassNotFoundException ex) {
        log.error(AppLogMessage.create("#Retry - could not load retry exception map", ex));
      }
    }
    return maps;
  }
}
