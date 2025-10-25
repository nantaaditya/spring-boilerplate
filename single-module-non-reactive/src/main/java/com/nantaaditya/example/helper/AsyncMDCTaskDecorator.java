package com.nantaaditya.example.helper;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

@RequiredArgsConstructor
public class AsyncMDCTaskDecorator implements TaskDecorator {

  private final ObservationWrapper observationWrapper;

  @Override
  public Runnable decorate(Runnable runnable) {
    Map<String, String> contextMap = MDC.getCopyOfContextMap();
    return () -> {
      try {
        if (contextMap != null) {
          MDC.setContextMap(contextMap);
        }
        observationWrapper.wrap(runnable);
      } finally {
        MDC.clear();
      }
    };
  }
}