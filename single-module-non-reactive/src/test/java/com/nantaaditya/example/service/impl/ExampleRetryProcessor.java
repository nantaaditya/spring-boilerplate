package com.nantaaditya.example.service.impl;

import com.nantaaditya.example.entity.DeadLetterProcess;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExampleRetryProcessor extends AbstractRetryProcessorService {

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
    log.info("#DeadLetterProcess - payload {}", deadLetterProcess);
  }
}
