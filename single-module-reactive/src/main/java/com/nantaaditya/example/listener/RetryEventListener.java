package com.nantaaditya.example.listener;

import com.nantaaditya.example.helper.ErrorHelper;
import com.nantaaditya.example.helper.ReactorEventBusHelper;
import com.nantaaditya.example.helper.RetryHelper;
import com.nantaaditya.example.helper.SchedulerHelper;
import com.nantaaditya.example.properties.RetryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.util.retry.Retry.RetrySignal;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryEventListener {

  private final RetryProperties retryProperties;
  private final ReactorEventBusHelper reactorEventBusHelper;
  private final SchedulerHelper schedulerHelper;

  @EventListener(ApplicationReadyEvent.class)
  public void retryEventSubscriber() {
    if (retryProperties.configurations() == null) return;

    for (String retryKey : retryProperties.configurations().keySet()) {
      consumeEvent(retryKey, RetryHelper.BEFORE_SUFFIX_EVENT);
      consumeEvent(retryKey, RetryHelper.AFTER_SUFFIX_EVENT);
    }
  }

  private void consumeEvent(String retryName, String suffixEvent) {
    reactorEventBusHelper.<RetrySignal>consume(retryName + suffixEvent, schedulerHelper.from("retry-listener"))
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

}
