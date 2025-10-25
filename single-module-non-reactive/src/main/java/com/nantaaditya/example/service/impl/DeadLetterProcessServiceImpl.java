package com.nantaaditya.example.service.impl;

import com.nantaaditya.example.entity.DeadLetterProcess;
import com.nantaaditya.example.helper.DateTimeHelper;
import com.nantaaditya.example.helper.RetryProcessorHelper;
import com.nantaaditya.example.model.request.RetryDeadLetterProcessRequest;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import com.nantaaditya.example.service.internal.DeadLetterProcessService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterProcessServiceImpl implements DeadLetterProcessService {

  private final DeadLetterProcessRepository deadLetterProcessRepository;
  private final RetryProcessorHelper retryProcessorHelper;

  @Override
  @Async("defaultAsyncTaskExecutor")
  public void remove(int days) {
    LocalDateTime now = LocalDateTime.now();
    deadLetterProcessRepository.deleteByCreatedDateLessThanAndProcessedIsTrue(now.minusDays(days)
        .atZone(DateTimeHelper.ZONE_ID).toInstant().toEpochMilli());
  }

  @Override
  @Async("defaultAsyncTaskExecutor")
  public void retry(RetryDeadLetterProcessRequest request) {
    PageRequest pageRequest = PageRequest.of(0, request.size(),
        Sort.by(Direction.ASC, "createdDate"));

    Page<DeadLetterProcess> deadLetterProcessPage = deadLetterProcessRepository
        .findByProcessTypeAndProcessNameAndProcessed(request.processType(), request.processName(),
            false, pageRequest);

    if (!deadLetterProcessPage.hasContent()) {
      log.info("#DeadLetterProcess - no dead letter processes [{}] [{}] found",
          request.processType(), request.processName());
      return;
    }

    List<DeadLetterProcess> deadLetterProcesses = deadLetterProcessPage.getContent();
    executeRetryProcess(request, deadLetterProcesses);

  }

  public void executeRetryProcess(RetryDeadLetterProcessRequest request, List<DeadLetterProcess> deadLetterProcesses) {
    AbstractRetryProcessorService processor = retryProcessorHelper.getProcessor(request.processType(), request.processName());
    if (processor == null) {
      log.warn("#DeadLetterProcess - no retry processor handler found with {} - {}", request.processType(), request.processName());
      return;
    }
    processor.execute(deadLetterProcesses);
  }
}
