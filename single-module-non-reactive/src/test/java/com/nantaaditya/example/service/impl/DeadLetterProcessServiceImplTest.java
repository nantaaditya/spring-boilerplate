package com.nantaaditya.example.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nantaaditya.example.entity.DeadLetterProcess;
import com.nantaaditya.example.helper.RestSender;
import com.nantaaditya.example.helper.RestSenderHelper;
import com.nantaaditya.example.helper.RetryExhaustionNotifier;
import com.nantaaditya.example.helper.RetryProcessorHelper;
import com.nantaaditya.example.model.constant.RetryStatus;
import com.nantaaditya.example.model.dto.RetryHistoryContext;
import com.nantaaditya.example.model.request.RetryDeadLetterProcessRequest;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import com.nantaaditya.example.spi.RetryExhaustionSource;
import com.nantaaditya.example.spi.RetryOutcomeEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import tools.jackson.databind.ObjectMapper;

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

  @Mock
  private RetryExhaustionNotifier retryExhaustionNotifier;

  @Test
  void remove() {
    doNothing().when(deadLetterProcessRepository).deleteByCreatedDateBeforeAndStatus(
        any(LocalDateTime.class), eq(RetryStatus.SUCCESS.name()));

    deadLetterProcessService = new DeadLetterProcessServiceImpl(
        deadLetterProcessRepository, retryProcessorHelper, restSenderHelper, objectMapper,
        retryExhaustionNotifier
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
        deadLetterProcessRepository, retryProcessorHelper, restSenderHelper, objectMapper,
        retryExhaustionNotifier
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
        deadLetterProcessRepository, retryProcessorHelper, restSenderHelper, objectMapper,
        retryExhaustionNotifier
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
        deadLetterProcessRepository, retryProcessorHelper, restSenderHelper, objectMapper,
        retryExhaustionNotifier
    );
    deadLetterProcessService.retry(request);

    verify(deadLetterProcessRepository).findByProcessTypeAndProcessNameAndStatusIn(
        anyString(), anyString(), anySet(), any(PageRequest.class)
    );
    verify(deadLetterProcessRepository).saveAll(anyList());
    verify(retryProcessorHelper).getProcessor(anyString(), anyString());
    verify(deadLetterProcessRepository).save(any(DeadLetterProcess.class));
    verify(retryExhaustionNotifier).notifyReplaySuccess(any(RetryOutcomeEvent.class));
  }

  @Test
  void retry_unknownClient_notifiesExhausted() throws IOException {
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
        .clientName("unknown-client")
        .method(HttpMethod.POST.name())
        .path("/api")
        .headers(objectMapper.writeValueAsString(headers))
        .idempotencyKey("1")
        .processType("type")
        .processName("name")
        .retryCount(2)
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
        .thenReturn(null);

    ExampleProcessor exampleProcessor = new ExampleProcessor(deadLetterProcessRepository, objectMapper);
    when(retryProcessorHelper.getProcessor(anyString(), anyString()))
        .thenReturn(exampleProcessor);

    deadLetterProcessService = new DeadLetterProcessServiceImpl(
        deadLetterProcessRepository, retryProcessorHelper, restSenderHelper, objectMapper,
        retryExhaustionNotifier
    );
    deadLetterProcessService.retry(request);

    verify(retryExhaustionNotifier).notifyExhausted(argThat((RetryOutcomeEvent event) ->
        event.source() == RetryExhaustionSource.DEAD_LETTER_REPLAY));
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
        deadLetterProcessRepository, retryProcessorHelper, restSenderHelper, objectMapper,
        retryExhaustionNotifier
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
  void retry_processorReplay_success() throws IOException {
    DeadLetterProcess result = runProcessorReplayTest(false);
    assertEquals(RetryStatus.SUCCESS.name(), result.getStatus());
    verify(retryExhaustionNotifier).notifyReplaySuccess(any(RetryOutcomeEvent.class));
  }

  @Test
  void retry_processorReplay_error() throws IOException {
    DeadLetterProcess result = runProcessorReplayTest(true);
    assertEquals(RetryStatus.EXHAUSTED.name(), result.getStatus());
    verify(retryExhaustionNotifier).notifyExhausted(argThat((RetryOutcomeEvent event) ->
        event.source() == RetryExhaustionSource.DEAD_LETTER_REPLAY));
  }

  private DeadLetterProcess runProcessorReplayTest(boolean replayFails) throws IOException {
    RetryDeadLetterProcessRequest request = new RetryDeadLetterProcessRequest("type", "name", 1);

    RetryHistoryContext retryHistory = new RetryHistoryContext(0, "failed", "failed");
    List<RetryHistoryContext> retryHistories = new LinkedList<>();
    retryHistories.add(retryHistory);

    DeadLetterProcess deadLetterProcess = DeadLetterProcess.builder()
        .processType("type")
        .processName("name")
        .retryCount(0)
        .maxRetry(1)
        .status(RetryStatus.NEW.name())
        .retryHistories(objectMapper.writeValueAsBytes(retryHistories))
        .build();
    when(deadLetterProcessRepository.findByProcessTypeAndProcessNameAndStatusIn(
        anyString(), anyString(), anySet(), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(deadLetterProcess)));
    when(deadLetterProcessRepository.save(any(DeadLetterProcess.class)))
        .thenAnswer(answer -> answer.getArguments()[0]);
    when(deadLetterProcessRepository.saveAll(anyList()))
        .thenAnswer(answer -> answer.getArgument(0));
    when(retryProcessorHelper.getProcessor(anyString(), anyString()))
        .thenReturn(new ExampleProcessor(deadLetterProcessRepository, objectMapper, replayFails));

    deadLetterProcessService = new DeadLetterProcessServiceImpl(
        deadLetterProcessRepository, retryProcessorHelper, restSenderHelper, objectMapper,
        retryExhaustionNotifier
    );
    deadLetterProcessService.retry(request);

    ArgumentCaptor<DeadLetterProcess> captor = ArgumentCaptor.forClass(DeadLetterProcess.class);
    verify(deadLetterProcessRepository).saveAll(anyList());
    verify(deadLetterProcessRepository).save(captor.capture());
    return captor.getValue();
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
        deadLetterProcessRepository, retryProcessorHelper, restSenderHelper, objectMapper,
        retryExhaustionNotifier
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
  void retryById_notFound() {
    when(deadLetterProcessRepository.findById(99L)).thenReturn(Optional.empty());

    deadLetterProcessService = new DeadLetterProcessServiceImpl(
        deadLetterProcessRepository, retryProcessorHelper, restSenderHelper, objectMapper,
        retryExhaustionNotifier
    );

    assertThrows(NoSuchElementException.class, () -> deadLetterProcessService.retryById(99L));
    verify(deadLetterProcessRepository).findById(99L);
  }

  @Test
  void retryById_terminalStatus() throws IOException {
    RetryHistoryContext retryHistory = new RetryHistoryContext(0, "response", "error");
    List<RetryHistoryContext> retryHistories = new LinkedList<>();
    retryHistories.add(retryHistory);

    DeadLetterProcess deadLetterProcess = DeadLetterProcess.builder()
        .processType("type").processName("name")
        .retryCount(1).maxRetry(3)
        .status(RetryStatus.SUCCESS.name())
        .retryHistories(objectMapper.writeValueAsBytes(retryHistories))
        .build();
    when(deadLetterProcessRepository.findById(1L)).thenReturn(Optional.of(deadLetterProcess));

    deadLetterProcessService = new DeadLetterProcessServiceImpl(
        deadLetterProcessRepository, retryProcessorHelper, restSenderHelper, objectMapper,
        retryExhaustionNotifier
    );
    deadLetterProcessService.retryById(1L);

    verify(deadLetterProcessRepository).findById(1L);
  }

  @Test
  void retryById_processorNotFound() throws IOException {
    RetryHistoryContext retryHistory = new RetryHistoryContext(0, "response", "error");
    List<RetryHistoryContext> retryHistories = new LinkedList<>();
    retryHistories.add(retryHistory);

    DeadLetterProcess deadLetterProcess = DeadLetterProcess.builder()
        .processType("type").processName("name")
        .retryCount(0).maxRetry(3)
        .status(RetryStatus.NEW.name())
        .retryHistories(objectMapper.writeValueAsBytes(retryHistories))
        .build();
    when(deadLetterProcessRepository.findById(1L)).thenReturn(Optional.of(deadLetterProcess));
    when(retryProcessorHelper.getProcessor(anyString(), anyString())).thenReturn(null);

    deadLetterProcessService = new DeadLetterProcessServiceImpl(
        deadLetterProcessRepository, retryProcessorHelper, restSenderHelper, objectMapper,
        retryExhaustionNotifier
    );
    deadLetterProcessService.retryById(1L);

    verify(deadLetterProcessRepository).findById(1L);
    verify(retryProcessorHelper).getProcessor("type", "name");
  }

  @Test
  void retryById_success() throws IOException {
    RetryHistoryContext retryHistory = new RetryHistoryContext(0, "response", "error");
    List<RetryHistoryContext> retryHistories = new LinkedList<>();
    retryHistories.add(retryHistory);

    DeadLetterProcess deadLetterProcess = DeadLetterProcess.builder()
        .processType("type").processName("name")
        .retryCount(0).maxRetry(3)
        .status(RetryStatus.NEW.name())
        .retryHistories(objectMapper.writeValueAsBytes(retryHistories))
        .build();
    when(deadLetterProcessRepository.findById(1L)).thenReturn(Optional.of(deadLetterProcess));
    when(deadLetterProcessRepository.save(any(DeadLetterProcess.class)))
        .thenAnswer(answer -> answer.getArguments()[0]);
    when(deadLetterProcessRepository.saveAll(anyList()))
        .thenAnswer(answer -> answer.getArgument(0));
    when(retryProcessorHelper.getProcessor(anyString(), anyString()))
        .thenReturn(new ExampleProcessor(deadLetterProcessRepository, objectMapper, false));

    deadLetterProcessService = new DeadLetterProcessServiceImpl(
        deadLetterProcessRepository, retryProcessorHelper, restSenderHelper, objectMapper,
        retryExhaustionNotifier
    );
    deadLetterProcessService.retryById(1L);

    ArgumentCaptor<DeadLetterProcess> captor = ArgumentCaptor.forClass(DeadLetterProcess.class);
    verify(deadLetterProcessRepository).findById(1L);
    verify(deadLetterProcessRepository).saveAll(anyList());
    verify(deadLetterProcessRepository).save(captor.capture());
    assertEquals(RetryStatus.SUCCESS.name(), captor.getValue().getStatus());
  }

  @Log4j2
  static class ExampleProcessor extends AbstractRetryProcessorService {

    private final boolean replayThrows;

    public ExampleProcessor(DeadLetterProcessRepository deadLetterProcessRepository, ObjectMapper objectMapper) {
      this(deadLetterProcessRepository, objectMapper, false);
    }

    public ExampleProcessor(DeadLetterProcessRepository deadLetterProcessRepository, ObjectMapper objectMapper,
        boolean replayThrows) {
      super(deadLetterProcessRepository, objectMapper);
      this.replayThrows = replayThrows;
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
    public <T> boolean isSuccess(T response) {
      if (response instanceof ResponseEntity<?> re) {
        return re.getStatusCode().is2xxSuccessful();
      }
      return Boolean.TRUE.equals(response);
    }

    @Override
    public <T> void onSuccess(DeadLetterProcess deadLetterProcess, T response) {
    }

    @Override
    public void onError(DeadLetterProcess deadLetterProcess, Throwable throwable) {
    }

    @Override
    public <T> String toRetryHistoryResponse(T response) {
      if (response instanceof ResponseEntity<?> re) {
        return re.hasBody() ? String.valueOf(re.getBody()) : null;
      }
      return String.valueOf(response);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T replay(DeadLetterProcess deadLetterProcess) {
      return (T) (replayThrows ? Boolean.FALSE : Boolean.TRUE);
    }
  }
}