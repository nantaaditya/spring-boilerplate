package com.nantaaditya.example.helper;

import com.nantaaditya.example.entity.DeadLetterProcess;
import com.nantaaditya.example.model.constant.RetryStatus;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.model.dto.DeadLetterCapable;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class DeadLetterRejectedExecutionHandler implements RejectedExecutionHandler {

  private static final String FALLBACK_PROCESS_TYPE = "ASYNC_REJECTED";

  private final DeadLetterProcessRepository deadLetterProcessRepository;

  @Override
  public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
    log.warn(AppLogMessage.message("#AsyncExecutor - task rejected, saving to dead letter [{}]",
        task.getClass().getSimpleName()));
    try {
      deadLetterProcessRepository.save(buildRecord(task));
    } catch (Exception e) {
      log.error(AppLogMessage.message("#AsyncExecutor - failed to save rejected task to dead letter [{}]",
          task.getClass().getSimpleName()).error(e));
    }
  }

  private DeadLetterProcess buildRecord(Runnable task) {
    if (task instanceof DeadLetterCapable capable) {
      return DeadLetterProcess.builder()
          .processType(capable.getProcessType())
          .processName(capable.getProcessName())
          .payload(capable.getPayload())
          .retryCount(0)
          .maxRetry(0)
          .status(RetryStatus.NEW.name())
          .lastError("Task rejected: async executor queue full")
          .build();
    }
    return DeadLetterProcess.builder()
        .processType(FALLBACK_PROCESS_TYPE)
        .processName(task.getClass().getSimpleName())
        .retryCount(0)
        .maxRetry(0)
        .status(RetryStatus.NEW.name())
        .lastError("Task rejected: async executor queue full")
        .build();
  }
}
