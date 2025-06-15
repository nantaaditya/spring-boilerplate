package com.nantaaditya.example.service.abstraction;

import com.nantaaditya.example.entity.DeadLetterProcess;
import com.nantaaditya.example.helper.ContextHelper;
import com.nantaaditya.example.helper.CustomQueryHelper;
import com.nantaaditya.example.helper.ErrorHelper;
import com.nantaaditya.example.helper.TracerHelper;
import com.nantaaditya.example.model.constant.HeaderConstant;
import com.nantaaditya.example.model.dto.ContextDTO;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public abstract class AbstractRetryProcessorService {

  protected DeadLetterProcessRepository deadLetterProcessRepository;
  protected CustomQueryHelper customQueryHelper;
  protected ContextHelper contextHelper;
  protected TracerHelper tracerHelper;

  protected AbstractRetryProcessorService(
      DeadLetterProcessRepository deadLetterProcessRepository,
      CustomQueryHelper customQueryHelper,
      ContextHelper contextHelper,
      TracerHelper tracerHelper) {
    this.deadLetterProcessRepository = deadLetterProcessRepository;
    this.customQueryHelper = customQueryHelper;
    this.contextHelper = contextHelper;
    this.tracerHelper = tracerHelper;
  }

  public abstract String getProcessType();
  public abstract String getProcessName();
  protected abstract void doProcess(DeadLetterProcess deadLetterProcess);

  public void execute(List<DeadLetterProcess> deadLetterProcesses) {
    Flux.fromIterable(deadLetterProcesses)
      .flatMap(this::update)
      .doOnNext(this::doProcess)
      .subscribe(
        success -> log.info("#Retry - [{}] [{}] total {} retry processed",
          getProcessType(),
          getProcessName(),
          deadLetterProcesses.size()
        ),
        error -> log.error("#Retry - [{}] [{}] total {} retry processed, error {} cause {}",
          getProcessType(),
          getProcessName(),
          deadLetterProcesses.size(),
          error.getMessage(),
          ErrorHelper.getRootCause(error)
        )
      );
  }

  private Mono<DeadLetterProcess> update(DeadLetterProcess deadLetterProcess) {
    deadLetterProcess.setProcessed(true);
    deadLetterProcess.setUpdatedBy(getUpdatedBy());
    deadLetterProcess.setUpdatedDate(System.currentTimeMillis());
    return deadLetterProcessRepository.save(deadLetterProcess);
  }

  private String getUpdatedBy() {
    return Optional.ofNullable(tracerHelper.getBaggage(HeaderConstant.REQUEST_ID))
      .map(contextHelper::get)
      .map(ContextDTO::getClientId)
      .orElseGet(() -> "SYSTEM");
  }

}
