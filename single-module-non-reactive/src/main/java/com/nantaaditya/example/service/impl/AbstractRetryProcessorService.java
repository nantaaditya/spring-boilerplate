package com.nantaaditya.example.service.impl;

import com.nantaaditya.example.entity.DeadLetterProcess;
import com.nantaaditya.example.model.constant.RetryStatus;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.model.dto.RetryHistoryContext;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Log4j2
public abstract class AbstractRetryProcessorService {

  protected DeadLetterProcessRepository deadLetterProcessRepository;
  protected ObjectMapper objectMapper;

  @Getter
  private final AtomicInteger successCounter = new AtomicInteger(0);
  @Getter
  private final AtomicInteger failedCounter = new AtomicInteger(0);
  @Getter
  private final AtomicInteger notEligibleCounter = new AtomicInteger(0);

  protected AbstractRetryProcessorService(DeadLetterProcessRepository deadLetterProcessRepository,
      ObjectMapper objectMapper) {
    this.deadLetterProcessRepository = deadLetterProcessRepository;
    this.objectMapper = objectMapper;
  }

  public abstract String getProcessType();
  public abstract String getProcessName();
  public abstract boolean isEligibleToBeRetried(DeadLetterProcess deadLetterProcess);
  public abstract <T> boolean isSuccess(T response);
  public abstract <T> void onSuccess(DeadLetterProcess deadLetterProcess, T response);
  public abstract void onError(DeadLetterProcess deadLetterProcess, Throwable throwable);
  public abstract <T> String toRetryHistoryResponse(T response);

  public <T> T replay(DeadLetterProcess deadLetterProcess) {
    throw new UnsupportedOperationException(
        "Override replay() to implement custom replay logic for " + getProcessType() + "/" + getProcessName());
  }

  public final void resetCounter() {
    successCounter.setRelease(0);
    failedCounter.setRelease(0);
    notEligibleCounter.setRelease(0);
  }

  public final <T> void update(DeadLetterProcess deadLetterProcess, T response, Throwable throwable) {
    if (isSuccess(response)) {
      deadLetterProcess.setStatus(RetryStatus.SUCCESS.name());
      successCounter.incrementAndGet();
    } else {
      boolean isMaxRetry = deadLetterProcess.getRetryCount() + 1 < deadLetterProcess.getMaxRetry();
      deadLetterProcess.setStatus(isMaxRetry ? RetryStatus.FAILED.name() : RetryStatus.EXHAUSTED.name());
      failedCounter.incrementAndGet();
    }

    Optional.ofNullable(throwable)
        .ifPresent(t -> deadLetterProcess.setLastError(t.getMessage()));
    updateRetryHistories(deadLetterProcess, response, throwable);
    deadLetterProcess.markRetry();
    deadLetterProcessRepository.save(deadLetterProcess);
  }

  private <T> void updateRetryHistories(DeadLetterProcess deadLetterProcess, T response,
      Throwable throwable) {
    try {
      List<RetryHistoryContext> retryHistories = objectMapper.readValue(deadLetterProcess.getRetryHistories(),
          new TypeReference<List<RetryHistoryContext>>(){});
      retryHistories.add(new RetryHistoryContext(
          deadLetterProcess.getRetryCount() + 1,
          toRetryHistoryResponse(response),
          throwable != null ? throwable.getMessage() : null
      ));
      deadLetterProcess.setRetryHistories(objectMapper.writeValueAsBytes(retryHistories));
    } catch (Exception e) {
      log.error(AppLogMessage.message("#Retry - failed to update retry histories {}", deadLetterProcess.getId()).error(e));
    }
  }

}
