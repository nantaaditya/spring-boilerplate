package com.nantaaditya.example.listener;

import com.nantaaditya.example.helper.ErrorHelper;
import com.nantaaditya.example.helper.ReactorEventBusHelper;
import com.nantaaditya.example.helper.RetryHelper;
import com.nantaaditya.example.properties.RetryProperties;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry.RetrySignal;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryEventListener {

  private final RetryProperties retryProperties;
  private final ReactorEventBusHelper reactorEventBusHelper;
  private final Map<String, Scheduler> schedulers = new ConcurrentHashMap<>();

  private static final int DEFAULT_PARALLEL_SCHEDULER = 10;

  @EventListener(ApplicationReadyEvent.class)
  public void retryEventSubscriber() {
    for (String retryKey : retryProperties.configurations().keySet()) {
      consumeEvent(retryKey, RetryHelper.BEFORE_SUFFIX_EVENT);
      consumeEvent(retryKey, RetryHelper.AFTER_SUFFIX_EVENT);
    }
  }

  private void consumeEvent(String retryName, String suffixEvent) {
    reactorEventBusHelper.<RetrySignal>consume(retryName + suffixEvent, getScheduler(retryName))
        .doOnNext(retrySignal -> {
          log.info("#Retry - name {} event {}", retryName + suffixEvent, retrySignal.totalRetries() + 1);
          retrySignal.retryContextView()
              .forEach((key, value) -> log.info("#Retry - name {} context {} - {}", retryName, key, value));
        })
        .subscribe(
            success -> log.debug("#Retry - success consume {}", retryName + suffixEvent),
            error -> log.error("#Retry - failed consume {}, error {} cause {}",
                retryName + suffixEvent, error.getMessage(), ErrorHelper.getRootCause(error))
        );
  }

  private Scheduler getScheduler(String name) {
    Scheduler scheduler = schedulers.get(name);
    if (scheduler == null) {
      scheduler = Schedulers.newParallel(name, DEFAULT_PARALLEL_SCHEDULER);
      schedulers.put(name, scheduler);
    }
    return scheduler;
  }
}
