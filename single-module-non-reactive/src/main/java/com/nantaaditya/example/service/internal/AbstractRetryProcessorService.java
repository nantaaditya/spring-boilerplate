package com.nantaaditya.example.service.internal;

import com.nantaaditya.example.entity.DeadLetterProcess;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractRetryProcessorService {

  protected DeadLetterProcessRepository deadLetterProcessRepository;

  public AbstractRetryProcessorService(DeadLetterProcessRepository deadLetterProcessRepository) {
    this.deadLetterProcessRepository = deadLetterProcessRepository;
  }

  public abstract String getProcessType();
  public abstract String getProcessName();
  protected abstract void doProcess(DeadLetterProcess deadLetterProcess);

  public void execute(List<DeadLetterProcess> deadLetterProcesses) {
    for (DeadLetterProcess deadLetterProcess : deadLetterProcesses) {
      update(deadLetterProcess);
      doProcess(deadLetterProcess);
    }
    log.info("#Retry - [{}] [{}] total {} retry processed", getProcessType(), getProcessName(), deadLetterProcesses.size());
  }

  private void update(DeadLetterProcess deadLetterProcess) {
    deadLetterProcess.setProcessed(true);
    deadLetterProcess.setUpdatedBy("internal-retry-process");
    deadLetterProcess.setUpdatedDate(System.currentTimeMillis());
    deadLetterProcessRepository.save(deadLetterProcess);
  }

}
