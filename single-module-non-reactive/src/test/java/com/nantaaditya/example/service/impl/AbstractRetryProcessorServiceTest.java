package com.nantaaditya.example.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nantaaditya.example.entity.DeadLetterProcess;
import com.nantaaditya.example.model.constant.RetryStatus;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.model.dto.RetryHistoryContext;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Log4j2
@ExtendWith(MockitoExtension.class)
class AbstractRetryProcessorServiceTest {

  private DeadLetterProcessRepository deadLetterProcessRepository = mock(
      DeadLetterProcessRepository.class);

  private ObjectMapper objectMapper = mock(ObjectMapper.class);

  private ArgumentCaptor<DeadLetterProcess> argumentCaptor = ArgumentCaptor.forClass(
      DeadLetterProcess.class);

  private static class ExampleRetryProcessor extends AbstractRetryProcessorService {

    public ExampleRetryProcessor(DeadLetterProcessRepository deadLetterProcessRepository,
        ObjectMapper objectMapper) {
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
      return true;
    }

    @Override
    public <T> void onSuccess(DeadLetterProcess deadLetterProcess, ResponseEntity<T> response) {
      log.info(AppLogMessage.message("success retry").additionalData(response.getBody()));
    }

    @Override
    public void onError(DeadLetterProcess deadLetterProcess, Throwable throwable) {
      log.error(AppLogMessage.message("failed retry").error(throwable));
    }
  }

  private ExampleRetryProcessor exampleRetryProcessor = new ExampleRetryProcessor(
      deadLetterProcessRepository, objectMapper);

  @Test
  void resetCounterTest() {
    exampleRetryProcessor.resetCounter();
  }

  @Test
  void updateSuccess() throws IOException {
    ObjectMapper realObjectMapper = new ObjectMapper();
    List<RetryHistoryContext> histories = new LinkedList<>();
    histories.add(new RetryHistoryContext(0, "response", "lastError"));
    byte[] historiesByte = realObjectMapper.writeValueAsBytes(histories);

    DeadLetterProcess deadLetterProcess = DeadLetterProcess.builder()
        .id(1L)
        .processType("type")
        .processName("name")
        .idempotencyKey("1")
        .clientName("client")
        .status(RetryStatus.NEW.name())
        .retryHistories(historiesByte)
        .build();

    when(deadLetterProcessRepository.save(any(DeadLetterProcess.class)))
        .thenAnswer(answer -> answer.getArgument(0));
    when(objectMapper.readValue(any(byte[].class), any(TypeReference.class)))
        .thenReturn(histories);
    when(objectMapper.writeValueAsString(anyString()))
        .thenReturn("OK");
    when(objectMapper.writeValueAsBytes(anyList()))
        .thenReturn(historiesByte);

    exampleRetryProcessor.update(
        deadLetterProcess,
        ResponseEntity.ok("OK"),
        null
    );

    verify(deadLetterProcessRepository).save(argumentCaptor.capture());

    DeadLetterProcess result = argumentCaptor.getValue();
    assertEquals(1L, result.getId());
    assertEquals("type", result.getProcessType());
    assertEquals(RetryStatus.SUCCESS.name(), result.getStatus());
    assertEquals("internal-retry-process", deadLetterProcess.getUpdatedBy());
  }

  @Test
  void updateFailed() throws IOException {
    ObjectMapper realObjectMapper = new ObjectMapper();
    List<RetryHistoryContext> histories = new LinkedList<>();
    histories.add(new RetryHistoryContext(0, "response", "lastError"));
    byte[] historiesByte = realObjectMapper.writeValueAsBytes(histories);

    DeadLetterProcess deadLetterProcess = DeadLetterProcess.builder()
        .id(1L)
        .processType("type")
        .processName("name")
        .idempotencyKey("1")
        .clientName("client")
        .status(RetryStatus.NEW.name())
        .retryHistories(historiesByte)
        .retryCount(0)
        .maxRetry(3)
        .build();

    when(deadLetterProcessRepository.save(any(DeadLetterProcess.class)))
        .thenAnswer(answer -> answer.getArgument(0));
    when(objectMapper.readValue(any(byte[].class), any(TypeReference.class)))
        .thenReturn(histories);
    when(objectMapper.writeValueAsString(anyString()))
        .thenReturn("ERROR");
    when(objectMapper.writeValueAsBytes(anyList()))
        .thenReturn(historiesByte);

    exampleRetryProcessor.update(
        deadLetterProcess,
        ResponseEntity.badRequest().body("ERROR"),
        null
    );

    verify(deadLetterProcessRepository).save(argumentCaptor.capture());

    DeadLetterProcess result = argumentCaptor.getValue();
    assertEquals(1L, result.getId());
    assertEquals("type", result.getProcessType());
    assertEquals(RetryStatus.FAILED.name(), result.getStatus());
    assertEquals("internal-retry-process", deadLetterProcess.getUpdatedBy());
  }
}