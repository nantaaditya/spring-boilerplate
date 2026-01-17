package com.nantaaditya.example.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nantaaditya.example.entity.DeadLetterProcess;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;

@Log4j2
public class ExampleRetryProcessor extends AbstractRetryProcessorService {

  public ExampleRetryProcessor(DeadLetterProcessRepository deadLetterProcessRepository, ObjectMapper objectMapper) {
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
    log.info(AppLogMessage.message("success"));
  }

  @Override
  public void onError(DeadLetterProcess deadLetterProcess, Throwable throwable) {
    log.error(AppLogMessage.message("error").error(throwable));
  }

}
