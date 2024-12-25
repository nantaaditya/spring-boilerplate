package com.nantaaditya.example.service.impl;

import com.nantaaditya.example.entity.DeadLetterProcess;
import com.nantaaditya.example.helper.DateTimeHelper;
import com.nantaaditya.example.helper.RetryProcessorHelper;
import com.nantaaditya.example.model.request.RetryDeadLetterProcessRequest;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import com.nantaaditya.example.service.internal.DeadLetterProcessService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
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

  private final ApplicationContext applicationContext;
  private final DeadLetterProcessRepository deadLetterProcessRepository;
  private final RetryProcessorHelper retryProcessorHelper;

  @Async
  @Override
  public void remove(int days) {
    LocalDateTime now = LocalDateTime.now();
    deadLetterProcessRepository.deleteByCreatedDateLessThan(now.minusDays(days)
        .atZone(DateTimeHelper.ZONE_ID).toInstant().toEpochMilli());
  }

  @Override
  @Async
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

    Map<String, List<DeadLetterProcess>> deadLetterProcesses = deadLetterProcessPage.getContent()
        .stream()
        .collect(Collectors.groupingBy(d -> d.getProcessType() + "|" + d.getProcessName()));

    // proxy bean
    DeadLetterProcessServiceImpl deadLetterProcessService = applicationContext.getBean(DeadLetterProcessServiceImpl.class);
    for (Map.Entry<String, List<DeadLetterProcess>> entry : deadLetterProcesses.entrySet()) {
      deadLetterProcessService.executeRetryProcess(entry.getKey(), entry.getValue());
    }
  }

  @Async
  public void executeRetryProcess(String key, List<DeadLetterProcess> deadLetterProcesses) {
    String [] processKey = key.split("\\|");
    AbstractRetryProcessorService processor = retryProcessorHelper.getProcessor(processKey[0], processKey[1]);
    processor.execute(deadLetterProcesses);
  }
}
