package com.nantaaditya.example.service.impl;

import com.nantaaditya.example.entity.DeadLetterProcess;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.ObjectMapper;

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
      try {
        return re.hasBody() ? objectMapper.writeValueAsString(re.getBody()) : null;
      } catch (Exception e) {
        return null;
      }
    }
    return String.valueOf(response);
  }

}
