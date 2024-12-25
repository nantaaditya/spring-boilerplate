package com.nantaaditya.example.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nantaaditya.example.entity.DeadLetterProcess;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@Slf4j
@ExtendWith(MockitoExtension.class)
class AbstractRetryProcessorServiceTest {

  private DeadLetterProcessRepository deadLetterProcessRepository = mock(DeadLetterProcessRepository.class);

  private ArgumentCaptor<DeadLetterProcess> argumentCaptor = ArgumentCaptor.forClass(DeadLetterProcess.class);

  private static class ExampleRetryProcessor extends AbstractRetryProcessorService {

    public ExampleRetryProcessor(DeadLetterProcessRepository deadLetterProcessRepository) {
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
      log.info("retry content {}", deadLetterProcess);
    }
  }

  @Test
  void execute() {
    ExampleRetryProcessor exampleRetryProcessor = new ExampleRetryProcessor(deadLetterProcessRepository);

    List<DeadLetterProcess> deadLetterProcesses = List.of(DeadLetterProcess.builder()
            .id(1L)
            .processType("type")
            .processName("name")
            .processed(false)
            .version(0l)
        .build());

    when(deadLetterProcessRepository.save(any(DeadLetterProcess.class)))
        .thenAnswer(answer -> answer.getArgument(0));

    exampleRetryProcessor.execute(deadLetterProcesses);

    verify(deadLetterProcessRepository).save(argumentCaptor.capture());

    DeadLetterProcess deadLetterProcess = argumentCaptor.getValue();
    assertEquals(1L, deadLetterProcess.getId());
    assertEquals("type", deadLetterProcess.getProcessType());
    assertTrue(deadLetterProcess.isProcessed());
    assertEquals("internal-retry-process", deadLetterProcess.getUpdatedBy());
  }
}