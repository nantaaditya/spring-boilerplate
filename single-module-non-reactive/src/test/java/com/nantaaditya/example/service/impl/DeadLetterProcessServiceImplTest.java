package com.nantaaditya.example.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nantaaditya.example.entity.DeadLetterProcess;
import com.nantaaditya.example.helper.RestSender;
import com.nantaaditya.example.helper.RestSenderHelper;
import com.nantaaditya.example.helper.RetryProcessorHelper;
import com.nantaaditya.example.model.constant.RetryStatus;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.model.dto.RetryHistoryContext;
import com.nantaaditya.example.model.request.RetryDeadLetterProcessRequest;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;

@Log4j2
@ExtendWith(MockitoExtension.class)
class DeadLetterProcessServiceImplTest {

  private DeadLetterProcessServiceImpl deadLetterProcessService;

  @Mock
  private DeadLetterProcessRepository deadLetterProcessRepository;

  @Mock
  private RetryProcessorHelper retryProcessorHelper;

  private ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  private RestSenderHelper restSenderHelper;

  @Mock
  private RestSender restSender;

  @Test
  void remove() {
    doNothing().when(deadLetterProcessRepository).deleteByCreatedDateBeforeAndStatus(
        any(LocalDateTime.class), eq(RetryStatus.SUCCESS.name()));

    deadLetterProcessService = new DeadLetterProcessServiceImpl(
        deadLetterProcessRepository, retryProcessorHelper, restSenderHelper, objectMapper
    );
    deadLetterProcessService.remove(30);

    verify(deadLetterProcessRepository).deleteByCreatedDateBeforeAndStatus(
        any(LocalDateTime.class), eq(RetryStatus.SUCCESS.name()));
  }

  @Test
  void retry_noContent() {
    RetryDeadLetterProcessRequest request = new RetryDeadLetterProcessRequest(
        "type", "name", 1
    );

    Page<DeadLetterProcess> processes = new PageImpl<>(List.of());
    when(deadLetterProcessRepository.findByProcessTypeAndProcessNameAndStatusIn(
        anyString(), anyString(), anySet(), any(PageRequest.class)))
        .thenReturn(processes);

    deadLetterProcessService = new DeadLetterProcessServiceImpl(
        deadLetterProcessRepository, retryProcessorHelper, restSenderHelper, objectMapper
    );
    deadLetterProcessService.retry(request);

    verify(deadLetterProcessRepository).findByProcessTypeAndProcessNameAndStatusIn(
        anyString(), anyString(), anySet(), any(PageRequest.class)
    );
  }

  @Test
  void retry_processorNotFound() throws IOException {
    RetryDeadLetterProcessRequest request = new RetryDeadLetterProcessRequest(
        "type", "name", 1
    );

    LinkedMultiValueMap headers = new LinkedMultiValueMap();
    headers.add("Accept", "application/json");
    headers.add("Content-Type", "application/json");

    RetryHistoryContext retryHistory = new RetryHistoryContext(
        0, "failed", "failed"
    );
    List<RetryHistoryContext> retryHistories = new LinkedList<>();
    retryHistories.add(retryHistory);

    DeadLetterProcess deadLetterProcess = DeadLetterProcess.builder()
        .clientName("client")
        .method(HttpMethod.POST.name())
        .path("/api")
        .headers(objectMapper.writeValueAsString(headers))
        .idempotencyKey("1")
        .processType("type")
        .processName("name")
        .retryCount(0)
        .maxRetry(3)
        .status(RetryStatus.NEW.name())
        .retryHistories(objectMapper.writeValueAsBytes(retryHistories))
        .build();
    Page<DeadLetterProcess> processes = new PageImpl<>(List.of(deadLetterProcess));
    when(deadLetterProcessRepository.findByProcessTypeAndProcessNameAndStatusIn(
        anyString(), anyString(), anySet(), any(PageRequest.class)))
        .thenReturn(processes);

    ExampleProcessor exampleProcessor = new ExampleProcessor(deadLetterProcessRepository, objectMapper);
    when(retryProcessorHelper.getProcessor(anyString(), anyString()))
        .thenReturn(null);

    deadLetterProcessService = new DeadLetterProcessServiceImpl(
        deadLetterProcessRepository, retryProcessorHelper, restSenderHelper, objectMapper
    );
    deadLetterProcessService.retry(request);

    verify(deadLetterProcessRepository).findByProcessTypeAndProcessNameAndStatusIn(
        anyString(), anyString(), anySet(), any(PageRequest.class)
    );
    verify(retryProcessorHelper).getProcessor(anyString(), anyString());
  }

  @Test
  void retry() throws IOException {
    RetryDeadLetterProcessRequest request = new RetryDeadLetterProcessRequest(
        "type", "name", 1
    );

    LinkedMultiValueMap headers = new LinkedMultiValueMap();
    headers.add("Accept", "application/json");
    headers.add("Content-Type", "application/json");

    RetryHistoryContext retryHistory = new RetryHistoryContext(
        0, "failed", "failed"
    );
    List<RetryHistoryContext> retryHistories = new LinkedList<>();
    retryHistories.add(retryHistory);

    DeadLetterProcess deadLetterProcess = DeadLetterProcess.builder()
        .clientName("client")
        .method(HttpMethod.POST.name())
        .path("/api")
        .headers(objectMapper.writeValueAsString(headers))
        .idempotencyKey("1")
        .processType("type")
        .processName("name")
        .retryCount(0)
        .maxRetry(3)
        .status(RetryStatus.NEW.name())
        .retryHistories(objectMapper.writeValueAsBytes(retryHistories))
        .build();
    Page<DeadLetterProcess> processes = new PageImpl<>(List.of(deadLetterProcess));
    when(deadLetterProcessRepository.findByProcessTypeAndProcessNameAndStatusIn(
        anyString(), anyString(), anySet(), any(PageRequest.class)))
        .thenReturn(processes);
    when(deadLetterProcessRepository.save(any(DeadLetterProcess.class)))
        .thenAnswer(answer -> answer.getArguments()[0]);
    when(deadLetterProcessRepository.saveAll(anyList()))
        .thenAnswer(answer -> answer.getArgument(0));
    when(restSenderHelper.getRestSender(deadLetterProcess.getClientName()))
        .thenReturn(restSender);
    when(restSender.execute(
        eq(HttpMethod.POST),
        eq("/api"),
        any(HttpHeaders.class), // or capture and assert later
        isNull(),
        any(ParameterizedTypeReference.class)
    )).thenReturn(ResponseEntity.ok("OK"));

    ExampleProcessor exampleProcessor = new ExampleProcessor(deadLetterProcessRepository, objectMapper);
    when(retryProcessorHelper.getProcessor(anyString(), anyString()))
        .thenReturn(exampleProcessor);

    deadLetterProcessService = new DeadLetterProcessServiceImpl(
        deadLetterProcessRepository, retryProcessorHelper, restSenderHelper, objectMapper
    );
    deadLetterProcessService.retry(request);

    verify(deadLetterProcessRepository).findByProcessTypeAndProcessNameAndStatusIn(
        anyString(), anyString(), anySet(), any(PageRequest.class)
    );
    verify(deadLetterProcessRepository).saveAll(anyList());
    verify(retryProcessorHelper).getProcessor(anyString(), anyString());
    verify(deadLetterProcessRepository).save(any(DeadLetterProcess.class));
  }

  @Test
  void retry_notEligible() throws IOException {
    RetryDeadLetterProcessRequest request = new RetryDeadLetterProcessRequest(
        "type", "name", 1
    );

    LinkedMultiValueMap headers = new LinkedMultiValueMap();
    headers.add("Accept", "application/json");
    headers.add("Content-Type", "application/json");

    RetryHistoryContext retryHistory = new RetryHistoryContext(
        0, "failed", "failed"
    );
    List<RetryHistoryContext> retryHistories = new LinkedList<>();
    retryHistories.add(retryHistory);

    DeadLetterProcess deadLetterProcess = DeadLetterProcess.builder()
        .clientName("client")
        .method(HttpMethod.POST.name())
        .path("/api")
        .headers(objectMapper.writeValueAsString(headers))
        .idempotencyKey("1")
        .processType("type")
        .processName("name")
        .retryCount(0)
        .maxRetry(3)
        .status(RetryStatus.NEW.name())
        .retryHistories(objectMapper.writeValueAsBytes(retryHistories))
        .payload("false".getBytes(StandardCharsets.UTF_8))
        .build();
    Page<DeadLetterProcess> processes = new PageImpl<>(List.of(deadLetterProcess));
    when(deadLetterProcessRepository.findByProcessTypeAndProcessNameAndStatusIn(
        anyString(), anyString(), anySet(), any(PageRequest.class)))
        .thenReturn(processes);
    when(deadLetterProcessRepository.save(any(DeadLetterProcess.class)))
        .thenAnswer(answer -> answer.getArguments()[0]);
    when(deadLetterProcessRepository.saveAll(anyList()))
        .thenAnswer(answer -> answer.getArgument(0));

    ExampleProcessor exampleProcessor = new ExampleProcessor(deadLetterProcessRepository, objectMapper);
    when(retryProcessorHelper.getProcessor(anyString(), anyString()))
        .thenReturn(exampleProcessor);

    deadLetterProcessService = new DeadLetterProcessServiceImpl(
        deadLetterProcessRepository, retryProcessorHelper, restSenderHelper, objectMapper
    );
    deadLetterProcessService.retry(request);

    verify(deadLetterProcessRepository).findByProcessTypeAndProcessNameAndStatusIn(
        anyString(), anyString(), anySet(), any(PageRequest.class)
    );
    verify(deadLetterProcessRepository).saveAll(anyList());
    verify(retryProcessorHelper).getProcessor(anyString(), anyString());
    verify(deadLetterProcessRepository).save(any(DeadLetterProcess.class));
  }

  @Test
  void retry_error() throws IOException {
    RetryDeadLetterProcessRequest request = new RetryDeadLetterProcessRequest(
        "type", "name", 1
    );

    LinkedMultiValueMap headers = new LinkedMultiValueMap();
    headers.add("Accept", "application/json");
    headers.add("Content-Type", "application/json");

    RetryHistoryContext retryHistory = new RetryHistoryContext(
        0, "failed", "failed"
    );
    List<RetryHistoryContext> retryHistories = new LinkedList<>();
    retryHistories.add(retryHistory);

    DeadLetterProcess deadLetterProcess = DeadLetterProcess.builder()
        .clientName("client")
        .method(HttpMethod.POST.name())
        .path("/api")
        .headers(objectMapper.writeValueAsString(headers))
        .idempotencyKey("1")
        .processType("type")
        .processName("name")
        .retryCount(0)
        .maxRetry(3)
        .status(RetryStatus.NEW.name())
        .retryHistories(objectMapper.writeValueAsBytes(retryHistories))
        .build();
    Page<DeadLetterProcess> processes = new PageImpl<>(List.of(deadLetterProcess));
    when(deadLetterProcessRepository.findByProcessTypeAndProcessNameAndStatusIn(
        anyString(), anyString(), anySet(), any(PageRequest.class)))
        .thenReturn(processes);
    when(deadLetterProcessRepository.save(any(DeadLetterProcess.class)))
        .thenAnswer(answer -> answer.getArguments()[0]);
    when(deadLetterProcessRepository.saveAll(anyList()))
        .thenAnswer(answer -> answer.getArgument(0));
    when(restSenderHelper.getRestSender(deadLetterProcess.getClientName()))
        .thenReturn(restSender);
    doThrow(RuntimeException.class).when(restSender).execute(
        eq(HttpMethod.POST),
        eq("/api"),
        any(HttpHeaders.class), // or capture and assert later
        isNull(),
        any(ParameterizedTypeReference.class)
    );

    ExampleProcessor exampleProcessor = new ExampleProcessor(deadLetterProcessRepository, objectMapper);
    when(retryProcessorHelper.getProcessor(anyString(), anyString()))
        .thenReturn(exampleProcessor);

    deadLetterProcessService = new DeadLetterProcessServiceImpl(
        deadLetterProcessRepository, retryProcessorHelper, restSenderHelper, objectMapper
    );
    deadLetterProcessService.retry(request);

    verify(deadLetterProcessRepository).findByProcessTypeAndProcessNameAndStatusIn(
        anyString(), anyString(), anySet(), any(PageRequest.class)
    );
    verify(deadLetterProcessRepository).saveAll(anyList());
    verify(retryProcessorHelper).getProcessor(anyString(), anyString());
    verify(deadLetterProcessRepository).save(any(DeadLetterProcess.class));
  }

  @Log4j2
  static class ExampleProcessor extends AbstractRetryProcessorService {

    public ExampleProcessor(DeadLetterProcessRepository deadLetterProcessRepository, ObjectMapper objectMapper) {
      super(deadLetterProcessRepository, objectMapper);
    }

    @Override
    public String getProcessType() {
      return "type";
    }

    @Override
    public String getProcessName() {
      return "name";
    }

    @Override
    public boolean isEligibleToBeRetried(DeadLetterProcess deadLetterProcess) {
      return deadLetterProcess.getPayload() == null;
    }

    @Override
    public <T> void onSuccess(DeadLetterProcess deadLetterProcess, ResponseEntity<T> response) {
      log.info(AppLogMessage.message("success"));
    }

    @Override
    public void onError(DeadLetterProcess deadLetterProcess, Throwable throwable) {
      log.error(AppLogMessage.message("error").error(throwable));
    }

  }
}