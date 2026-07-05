package com.nantaaditya.example.helper;

import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.spi.RetryOutcomeEvent;
import com.nantaaditya.example.spi.RetryOutcomeListener;
import java.util.List;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class RetryExhaustionNotifier {

  private final List<RetryOutcomeListener> listeners;

  public void notifyExhausted(RetryOutcomeEvent event) {
    dispatch(RetryOutcomeListener::onRetryExhausted, event, "onRetryExhausted");
  }

  public void notifyReplaySuccess(RetryOutcomeEvent event) {
    dispatch(RetryOutcomeListener::onReplaySuccess, event, "onReplaySuccess");
  }

  private void dispatch(BiConsumer<RetryOutcomeListener, RetryOutcomeEvent> callback,
      RetryOutcomeEvent event, String callbackName) {
    for (RetryOutcomeListener listener : listeners) {
      try {
        callback.accept(listener, event);
      } catch (Exception e) {
        log.error(AppLogMessage.message("#Retry - listener {} failed on {} for {}/{}",
            listener.getClass().getSimpleName(), callbackName, event.processType(), event.processName()).error(e));
      }
    }
  }
}
