package com.nantaaditya.example.helper;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.nantaaditya.example.spi.RetryExhaustionSource;
import com.nantaaditya.example.spi.RetryOutcomeEvent;
import com.nantaaditya.example.spi.RetryOutcomeListener;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RetryExhaustionNotifierTest {

  private final RetryOutcomeEvent event = new RetryOutcomeEvent(
      "processType", "processName", "req-001", 3, 3, "boom",
      RetryExhaustionSource.INITIAL_RETRY, LocalDateTime.now());

  @Test
  void notifyExhausted_callsAllListeners() {
    RetryOutcomeListener first = mock(RetryOutcomeListener.class);
    RetryOutcomeListener second = mock(RetryOutcomeListener.class);
    RetryExhaustionNotifier notifier = new RetryExhaustionNotifier(List.of(first, second));

    notifier.notifyExhausted(event);

    verify(first).onRetryExhausted(event);
    verify(second).onRetryExhausted(event);
  }

  @Test
  void notifyExhausted_oneListenerThrows_othersStillCalled() {
    RetryOutcomeListener failing = mock(RetryOutcomeListener.class);
    RetryOutcomeListener healthy = mock(RetryOutcomeListener.class);
    doThrow(new RuntimeException("listener failure")).when(failing).onRetryExhausted(event);
    RetryExhaustionNotifier notifier = new RetryExhaustionNotifier(List.of(failing, healthy));

    notifier.notifyExhausted(event);

    verify(healthy).onRetryExhausted(event);
  }

  @Test
  void notifyExhausted_noListeners_doesNotThrow() {
    RetryExhaustionNotifier notifier = new RetryExhaustionNotifier(List.of());

    notifier.notifyExhausted(event);
  }

  @Test
  void notifyReplaySuccess_callsAllListeners() {
    RetryOutcomeListener first = mock(RetryOutcomeListener.class);
    RetryOutcomeListener second = mock(RetryOutcomeListener.class);
    RetryExhaustionNotifier notifier = new RetryExhaustionNotifier(List.of(first, second));

    notifier.notifyReplaySuccess(event);

    verify(first).onReplaySuccess(event);
    verify(second).onReplaySuccess(event);
  }

  @Test
  void notifyReplaySuccess_oneListenerThrows_othersStillCalled() {
    RetryOutcomeListener failing = mock(RetryOutcomeListener.class);
    RetryOutcomeListener healthy = mock(RetryOutcomeListener.class);
    doThrow(new RuntimeException("listener failure")).when(failing).onReplaySuccess(event);
    RetryExhaustionNotifier notifier = new RetryExhaustionNotifier(List.of(failing, healthy));

    notifier.notifyReplaySuccess(event);

    verify(healthy).onReplaySuccess(event);
  }
}
