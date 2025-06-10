package com.nantaaditya.example.helper;

import com.nantaaditya.example.model.dto.ContextDTO;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContextHelper {

  private final Map<String, ContextDTO> contexts = new ConcurrentHashMap<>();
  private final Map<String, String> additionalContexts = new ConcurrentHashMap<>();

  public void put(ContextDTO contextDTO) {
    try {
      contexts.put(contextDTO.getRequestId(), contextDTO);
    } catch (Exception ex) {
      log.error("#Context - failed to save {} cause {}", contextDTO.getRequestId(), ErrorHelper.getRootCause(ex));
    }
  }

  public void update(String requestId, UnaryOperator<ContextDTO> contextFunction) {
    ContextDTO contextDTO = get(requestId);
    if (contextDTO != null) {
      contextDTO = contextFunction.apply(contextDTO);
      put(contextDTO);
    }
  }

  public void put(String requestId, String additionalData) {
    try {
      additionalContexts.put(requestId, additionalData);
    } catch (Exception ex) {
      log.error("#Context - failed to update {} cause {}", requestId, ErrorHelper.getRootCause(ex));
    }
  }

  public ContextDTO get(String requestId) {
    try {
      return contexts.getOrDefault(requestId, null);
    } catch (Exception ex) {
      log.error("#MDC - failed to get {} cause {}", requestId, ErrorHelper.getRootCause(ex));
      return null;
    }
  }

  public byte[] getAdditionalData(String requestId) {
    String json = additionalContexts.getOrDefault(requestId, null);
    if (json == null) return null;

    return json.getBytes(StandardCharsets.UTF_8);
  }

  public void cleanUp(String requestId) {
    contexts.remove(requestId);
    additionalContexts.remove(requestId);
    MDC.clear();
  }

  public int size() {
    return contexts.size();
  }
}
