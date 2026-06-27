package com.nantaaditya.example.helper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.nantaaditya.example.entity.DeadLetterProcess;
import com.nantaaditya.example.model.constant.RetryStatus;
import com.nantaaditya.example.model.dto.DeadLetterCapable;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeadLetterRejectedExecutionHandlerTest {

  @Mock
  private DeadLetterProcessRepository deadLetterProcessRepository;

  @Mock
  private ThreadPoolExecutor executor;

  @InjectMocks
  private DeadLetterRejectedExecutionHandler handler;

  @Test
  void rejectedExecution_deadLetterCapableTask_savesWithProvidedMetadata() {
    Runnable task = new DeadLetterCapableTask("ORDER_PROCESS", "placeOrder", new byte[]{1, 2, 3});

    handler.rejectedExecution(task, executor);

    verify(deadLetterProcessRepository).save(argThat(record ->
        "ORDER_PROCESS".equals(record.getProcessType()) &&
        "placeOrder".equals(record.getProcessName()) &&
        record.getPayload() != null &&
        record.getRetryCount() == 0 &&
        RetryStatus.NEW.name().equals(record.getStatus())
    ));
  }

  @Test
  void rejectedExecution_plainRunnable_savesWithFallbackMetadata() {
    Runnable task = () -> {};

    handler.rejectedExecution(task, executor);

    verify(deadLetterProcessRepository).save(argThat(record ->
        "ASYNC_REJECTED".equals(record.getProcessType()) &&
        record.getProcessName() != null &&
        record.getPayload() == null &&
        record.getRetryCount() == 0 &&
        RetryStatus.NEW.name().equals(record.getStatus())
    ));
  }

  @Test
  void rejectedExecution_repositoryThrows_doesNotRethrow() {
    doThrow(new RuntimeException("db error"))
        .when(deadLetterProcessRepository).save(any(DeadLetterProcess.class));

    handler.rejectedExecution(() -> {}, executor);
    // no exception propagated — test passes if we reach here
  }

  private record DeadLetterCapableTask(
      String processType,
      String processName,
      byte[] payload
  ) implements Runnable, DeadLetterCapable {

    @Override
    public void run() {}

    @Override
    public String getProcessType() { return processType; }

    @Override
    public String getProcessName() { return processName; }

    @Override
    public byte[] getPayload() { return payload; }
  }
}
