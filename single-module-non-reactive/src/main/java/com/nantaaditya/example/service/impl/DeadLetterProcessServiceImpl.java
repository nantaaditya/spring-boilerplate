package com.nantaaditya.example.service.impl;

import com.nantaaditya.example.entity.DeadLetterProcess;
import com.nantaaditya.example.helper.DateTimeHelper;
import com.nantaaditya.example.helper.RestSender;
import com.nantaaditya.example.helper.RestSenderHelper;
import com.nantaaditya.example.helper.RetryExhaustionNotifier;
import com.nantaaditya.example.helper.RetryProcessorHelper;
import com.nantaaditya.example.model.constant.RetryStatus;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.model.request.RetryDeadLetterProcessRequest;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import com.nantaaditya.example.service.internal.DeadLetterProcessService;
import com.nantaaditya.example.spi.RetryExhaustionSource;
import com.nantaaditya.example.spi.RetryOutcomeEvent;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Log4j2
@Service
@RequiredArgsConstructor
public class DeadLetterProcessServiceImpl implements DeadLetterProcessService {

  private static final Set<String> TERMINAL_STATUSES = Set.of(
      RetryStatus.SUCCESS.name(), RetryStatus.EXHAUSTED.name()
  );

  private final DeadLetterProcessRepository deadLetterProcessRepository;
  private final RetryProcessorHelper retryProcessorHelper;
  private final RestSenderHelper restSenderHelper;
  private final ObjectMapper objectMapper;
  private final RetryExhaustionNotifier retryExhaustionNotifier;

  @Async("defaultAsyncTaskExecutor")
  @Override
  public void remove(int days) {
    LocalDateTime now = LocalDateTime.now(DateTimeHelper.ZONE_ID);
    deadLetterProcessRepository.deleteByCreatedDateBeforeAndStatus(now.minusDays(days),
        RetryStatus.SUCCESS.name());
  }

  @Override
  @Async("defaultAsyncTaskExecutor")
  public void retry(RetryDeadLetterProcessRequest request) {
    PageRequest pageRequest = PageRequest.of(0, request.size(),
        Sort.by(Direction.ASC, "createdDate"));

    Page<DeadLetterProcess> deadLetterProcessPage = deadLetterProcessRepository
        .findByProcessTypeAndProcessNameAndStatusIn(request.processType(), request.processName(),
            Set.of(RetryStatus.NEW.name(), RetryStatus.FAILED.name()), pageRequest);

    if (!deadLetterProcessPage.hasContent()) {
      log.info(
          AppLogMessage.message("#DeadLetterProcess - no dead letter processes [{}] [{}] found",
              request.processType(), request.processName()));
      return;
    }

    List<DeadLetterProcess> deadLetterProcesses = getRetryableDeadLetterProcess(
        deadLetterProcessPage);
    executeRetryProcess(request, deadLetterProcesses);
  }

  private List<DeadLetterProcess> getRetryableDeadLetterProcess(
      Page<DeadLetterProcess> deadLetterProcessPage) {
    return deadLetterProcessPage.getContent()
        .stream()
        .filter(d -> d.getRetryCount() < d.getMaxRetry())
        .toList();
  }

  public void executeRetryProcess(RetryDeadLetterProcessRequest request,
      List<DeadLetterProcess> deadLetterProcesses) {
    AbstractRetryProcessorService processor = retryProcessorHelper.getProcessor(
        request.processType(), request.processName());
    if (processor == null) {
      log.warn(AppLogMessage.message(
          "#DeadLetterProcess - no retry processor handler found with {} - {}",
          request.processType(), request.processName()));
      return;
    }

    updateInProgress(deadLetterProcesses);
    processor.resetCounter();

    for (DeadLetterProcess deadLetterProcess : deadLetterProcesses) {
      if (!processor.isEligibleToBeRetried(deadLetterProcess)) {
        deadLetterProcess.markAsSuccess();
        deadLetterProcessRepository.save(deadLetterProcess);
        processor.getNotEligibleCounter().incrementAndGet();
        log.warn(AppLogMessage.message("#DeadLetterProcess - {} is not eligible to be retried",
            deadLetterProcess.getId()));
        continue;
      } else {
        retry(processor, deadLetterProcess);
      }
    }
    log.info(AppLogMessage.message(
        "#DeadLetterProcess - total data {}, success {}, failed {}, not eligible {}",
        deadLetterProcesses.size(), processor.getSuccessCounter(), processor.getFailedCounter(),
        processor.getNotEligibleCounter()));
  }

  // clientName != null → record originated from a RestClient call; resend over HTTP.
  // clientName == null → record originated from async/retry infrastructure; delegate to the processor's replay().
  private void retry(AbstractRetryProcessorService processor, DeadLetterProcess deadLetterProcess) {
    if (deadLetterProcess.getClientName() != null) {
      executeHttpRetry(processor, deadLetterProcess);
    } else {
      executeProcessorRetry(processor, deadLetterProcess);
    }
  }

  private void executeHttpRetry(AbstractRetryProcessorService processor, DeadLetterProcess deadLetterProcess) {
    RestSender restSender = restSenderHelper.getRestSender(deadLetterProcess.getClientName());
    ResponseEntity<Object> response = ResponseEntity.internalServerError().build();
    if (restSender == null) {
      log.error(AppLogMessage.message("#DeadLetterProcess - no RestSender found for client {}", deadLetterProcess.getClientName()));
      notifyReplayOutcome(deadLetterProcess, processor.update(deadLetterProcess, response,
          new IllegalStateException("unknown client: " + deadLetterProcess.getClientName())));
      return;
    }
    try {
      response = restSender.execute(
          HttpMethod.valueOf(deadLetterProcess.getMethod()),
          deadLetterProcess.getPath(),
          constructHttpHeaders(deadLetterProcess),
          constructRequest(deadLetterProcess),
          new ParameterizedTypeReference<Object>() {}
      );
      processor.onSuccess(deadLetterProcess, response);
      notifyReplayOutcome(deadLetterProcess, processor.update(deadLetterProcess, response, null));
    } catch (Exception e) {
      processor.onError(deadLetterProcess, e);
      notifyReplayOutcome(deadLetterProcess, processor.update(deadLetterProcess, response, e));
    }
  }

  private <T> void executeProcessorRetry(AbstractRetryProcessorService processor, DeadLetterProcess deadLetterProcess) {
    T response = null;
    try {
      response = processor.replay(deadLetterProcess);
      processor.onSuccess(deadLetterProcess, response);
      notifyReplayOutcome(deadLetterProcess, processor.update(deadLetterProcess, response, null));
    } catch (Exception e) {
      processor.onError(deadLetterProcess, e);
      notifyReplayOutcome(deadLetterProcess, processor.update(deadLetterProcess, null, e));
    }
  }

  private void notifyReplayOutcome(DeadLetterProcess deadLetterProcess, RetryStatus status) {
    if (status != RetryStatus.EXHAUSTED && status != RetryStatus.SUCCESS) {
      return;
    }
    RetryOutcomeEvent event = new RetryOutcomeEvent(
        deadLetterProcess.getProcessType(),
        deadLetterProcess.getProcessName(),
        deadLetterProcess.getIdempotencyKey(),
        deadLetterProcess.getRetryCount(),
        deadLetterProcess.getMaxRetry(),
        deadLetterProcess.getLastError(),
        RetryExhaustionSource.DEAD_LETTER_REPLAY,
        LocalDateTime.now()
    );
    if (status == RetryStatus.EXHAUSTED) {
      retryExhaustionNotifier.notifyExhausted(event);
    } else {
      retryExhaustionNotifier.notifyReplaySuccess(event);
    }
  }

  private Object constructRequest(DeadLetterProcess deadLetterProcess) {
    try {
      if (deadLetterProcess.getPayload() == null || deadLetterProcess.getPayload().length == 0) {
        return null;
      }
      return objectMapper.readValue(deadLetterProcess.getPayload(), Object.class);
    } catch (Exception e) {
      log.warn(AppLogMessage.message("#DeadLetterProcess - failed to convert payload {}", deadLetterProcess.getId()).error(e));
      return null;
    }
  }

  private HttpHeaders constructHttpHeaders(DeadLetterProcess deadLetterProcess) {
    try {
      LinkedMultiValueMap<String, String> headers = objectMapper.readValue(
          deadLetterProcess.getHeaders(),
          new TypeReference<LinkedMultiValueMap<String, String>>() {
          });
      headers.add("x-retry-counter", String.valueOf(deadLetterProcess.getRetryCount() + 1));
      headers.add("x-retry-time",
          DateTimeHelper.getDateInFormat(ZonedDateTime.now(), DateTimeHelper.ISO_8601_GMT7_FORMAT));
      return new HttpHeaders(headers);
    } catch (JacksonException e) {
      log.error(AppLogMessage.message("#DeadLetterProcess - failed generate header").error(e));
      return new HttpHeaders();
    }
  }

  @Override
  @Async("defaultAsyncTaskExecutor")
  public void retryById(long id) {
    DeadLetterProcess deadLetterProcess = deadLetterProcessRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("dead_letter_process not found: id=" + id));

    if (TERMINAL_STATUSES.contains(deadLetterProcess.getStatus())) {
      log.warn(AppLogMessage.message("#DeadLetterProcess - id {} already in terminal status {}, skipping",
          id, deadLetterProcess.getStatus()));
      return;
    }

    if (deadLetterProcess.getRetryCount() >= deadLetterProcess.getMaxRetry()) {
      log.warn(AppLogMessage.message("#DeadLetterProcess - id {} retryCount {} >= maxRetry {}, skipping",
          id, deadLetterProcess.getRetryCount(), deadLetterProcess.getMaxRetry()));
      return;
    }

    AbstractRetryProcessorService processor = retryProcessorHelper.getProcessor(
        deadLetterProcess.getProcessType(), deadLetterProcess.getProcessName());
    if (processor == null) {
      log.warn(AppLogMessage.message(
          "#DeadLetterProcess - no retry processor handler found with {} - {}",
          deadLetterProcess.getProcessType(), deadLetterProcess.getProcessName()));
      return;
    }

    updateInProgress(List.of(deadLetterProcess));
    retry(processor, deadLetterProcess);
  }

  private void updateInProgress(List<DeadLetterProcess> deadLetterProcesses) {
    deadLetterProcesses
        .forEach(d -> d.setStatus(RetryStatus.RETRYING.name()));
    deadLetterProcessRepository.saveAll(deadLetterProcesses);
  }
}
