package com.nantaaditya.example.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nantaaditya.example.entity.DeadLetterProcess;
import com.nantaaditya.example.model.constant.RetryConstant;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;

@Log4j2
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
    log.debug(AppLogMessage.message("#RETRY - close retry [{}]", name).additionalData(getRetryContextAttribute(context)));
    saveExhaustedRetry(context);
  }

  @Override
  public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
      Throwable throwable) {
    log.error(AppLogMessage.message("#RETRY - error retry [{}]", name).error(throwable).additionalData(getRetryContextAttribute(context)));
  }

  @Override
  public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
    log.warn(AppLogMessage.message("#RETRY - open [{}]", name).additionalData(getRetryContextAttribute(context)));
    return true;
  }

  private void saveExhaustedRetry(RetryContext retryContext) {
    Throwable throwable = retryContext.getLastThrowable();
    if (throwable == null) return;

    try {
      log.error(AppLogMessage.message("#RETRY - last error").error(throwable));
      byte [] request = objectMapper.writeValueAsBytes(retryContext.getAttribute("request"));
      deadLetterProcessRepository.save(DeadLetterProcess.create(retryContext, request));
    } catch (Exception e) {
      log.error(AppLogMessage.message("#RETRY - failed to save exhausted retry")
              .error(e)
              .additionalData(getRetryContextAttribute(retryContext))
      );
    }
  }

  @SneakyThrows
  private Map<String, Object> getRetryContextAttribute(RetryContext retryContext) {
    Map<String, Object> attributes = new HashMap<>();
    for (String attributeName : retryContext.attributeNames()) {
      if (attributeName.equals(RetryConstant.EXCEPTION.getName())) continue;
      attributes.put(attributeName, retryContext.getAttribute(attributeName));
    }
    return attributes;
  }
}
