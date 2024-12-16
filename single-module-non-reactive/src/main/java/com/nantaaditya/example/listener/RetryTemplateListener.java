package com.nantaaditya.example.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nantaaditya.example.entity.DeadLetterProcess;
import com.nantaaditya.example.model.constant.RetryConstant;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;

@Slf4j
public class RetryTemplateListener implements RetryListener {

  private final String name;
  private final ObjectMapper objectMapper;
  private final DeadLetterProcessRepository deadLetterProcessRepository;

  public RetryTemplateListener(String name, ObjectMapper objectMapper, DeadLetterProcessRepository deadLetterProcessRepository) {
    this.name = name;
    this.objectMapper = objectMapper;
    this.deadLetterProcessRepository = deadLetterProcessRepository;
  }

  @Override
  public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
      Throwable throwable) {
    log.debug("#RETRY - close retry [{}] {}", name, getRetryContextAttribute(context));
    saveExhaustedRetry(context);
  }

  @Override
  public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
      Throwable throwable) {
    log.error("#RETRY - error retry [{}] {} ", name, getRetryContextAttribute(context));
  }

  @Override
  public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
    log.warn("#RETRY - open [{}] {}", name, getRetryContextAttribute(context));
    return true;
  }

  private void saveExhaustedRetry(RetryContext retryContext) {
    Throwable throwable = retryContext.getLastThrowable();
    if (throwable == null) return;

    try {
      log.error("#RETRY - last error {}", throwable.getMessage());
      byte [] request = objectMapper.writeValueAsBytes(retryContext.getAttribute("request"));
      deadLetterProcessRepository.save(DeadLetterProcess.create(retryContext, request));
    } catch (Exception e) {
      log.error("#RETRY - failed to save exhausted retry {}, ",
          getRetryContextAttribute(retryContext), e);
    }
  }

  @SneakyThrows
  private String getRetryContextAttribute(RetryContext retryContext) {
    Map<String, Object> attributes = new HashMap<>();
    for (String attributeName : retryContext.attributeNames()) {
      if (attributeName.equals(RetryConstant.EXCEPTION.getName())) continue;
      attributes.put(attributeName, retryContext.getAttribute(attributeName));
    }
    return objectMapper.writeValueAsString(attributes);
  }
}
