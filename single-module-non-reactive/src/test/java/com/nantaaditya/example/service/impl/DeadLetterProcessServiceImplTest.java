package com.nantaaditya.example.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nantaaditya.example.entity.DeadLetterProcess;
import com.nantaaditya.example.helper.RetryProcessorHelper;
import com.nantaaditya.example.model.request.RetryDeadLetterProcessRequest;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import com.nantaaditya.example.service.internal.AbstractRetryProcessorService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@Slf4j
@ExtendWith(MockitoExtension.class)
class DeadLetterProcessServiceImplTest {

  @InjectMocks
  private DeadLetterProcessServiceImpl deadLetterProcessService;

  @Mock
  private DeadLetterProcessRepository deadLetterProcessRepository;

  @Mock
  private ApplicationContext applicationContext;

  @Mock
  private RetryProcessorHelper retryProcessorHelper;

  @Test
  void remove() {
    doNothing().when(deadLetterProcessRepository).deleteByCreatedDateLessThan(anyLong());

    deadLetterProcessService.remove(30);

    verify(deadLetterProcessRepository).deleteByCreatedDateLessThan(anyLong());
  }

  @Test
  void retry_noContent() {
    RetryDeadLetterProcessRequest request = new RetryDeadLetterProcessRequest(
        "type", "name", 1
    );

    Page<DeadLetterProcess> processes = new PageImpl<>(List.of());
    when(deadLetterProcessRepository.findByProcessTypeAndProcessNameAndProcessed(
        anyString(), anyString(), eq(false), any(PageRequest.class)))
        .thenReturn(processes);

    deadLetterProcessService.retry(request);

    verify(deadLetterProcessRepository).findByProcessTypeAndProcessNameAndProcessed(
        anyString(), anyString(), eq(false), any(PageRequest.class)
    );
  }

  @Test
  void retry() {
    RetryDeadLetterProcessRequest request = new RetryDeadLetterProcessRequest(
        "type", "name", 1
    );

    DeadLetterProcess deadLetterProcess = DeadLetterProcess.builder()
        .processType("type")
        .processName("name")
        .processed(false)
        .build();
    Page<DeadLetterProcess> processes = new PageImpl<>(List.of(deadLetterProcess));
    when(deadLetterProcessRepository.findByProcessTypeAndProcessNameAndProcessed(
        anyString(), anyString(), eq(false), any(PageRequest.class)))
        .thenReturn(processes);

    when(applicationContext.getBean(DeadLetterProcessServiceImpl.class))
        .thenReturn(deadLetterProcessService);

    when(deadLetterProcessRepository.save(any(DeadLetterProcess.class)))
        .thenAnswer(answer -> answer.getArguments()[0]);

    ExampleProcessor exampleProcessor = new ExampleProcessor(deadLetterProcessRepository);

    when(retryProcessorHelper.getProcessor(anyString(), anyString()))
        .thenReturn(exampleProcessor);

    deadLetterProcessService.retry(request);

    verify(deadLetterProcessRepository).findByProcessTypeAndProcessNameAndProcessed(
        anyString(), anyString(), eq(false), any(PageRequest.class)
    );

    verify(applicationContext).getBean(DeadLetterProcessServiceImpl.class);

    verify(retryProcessorHelper).getProcessor(anyString(), anyString());

    verify(deadLetterProcessRepository).save(any(DeadLetterProcess.class));
  }

  static class ExampleProcessor extends AbstractRetryProcessorService {

    public ExampleProcessor(DeadLetterProcessRepository deadLetterProcessRepository) {
      super(deadLetterProcessRepository);
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
    protected void doProcess(DeadLetterProcess deadLetterProcess) {
      log.info("#DeadLLetterProcess - {}", deadLetterProcess);
    }
  }
}