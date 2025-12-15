package com.nantaaditya.example.service.impl;

import com.nantaaditya.example.entity.DeadLetterProcess;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import lombok.extern.log4j.Log4j2;

@Log4j2
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
    log.info(AppLogMessage.message("#DeadLetterProcess - payload").additionalData(deadLetterProcess));
  }
}
