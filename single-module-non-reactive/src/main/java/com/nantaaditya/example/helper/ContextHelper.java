package com.nantaaditya.example.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nantaaditya.example.model.constant.ContextConstant;
import com.nantaaditya.example.model.dto.ContextDTO;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

@Slf4j
public class ContextHelper {

  private static final ObjectMapper mapper = new ObjectMapper();

  private static final String CONTEXT_KEY = "context";
  private static final String ADDITIONAL_DATA_KEY = "additionalData";

  private ContextHelper() {}

  static {
    mapper.registerModule(new JavaTimeModule());
  }

  public static void put(ContextDTO contextDTO) {
    try {
      String json = mapper.writeValueAsString(contextDTO);
      MDC.put(ContextConstant.REQUEST_ID.getValue(), contextDTO.requestId());
      MDC.put(CONTEXT_KEY, json);
    } catch (JsonProcessingException ex) {
      log.error("#MDC - failed to save {}", CONTEXT_KEY, ex);
    }
  }

  public static void update(Function<ContextDTO, ContextDTO> contextFunction) {
    ContextDTO contextDTO = get();
    if (contextDTO != null) {
      contextDTO = contextFunction.apply(contextDTO);
      put(contextDTO);
    }
  }

  public static void put(String additionalData) {
    MDC.put(ADDITIONAL_DATA_KEY, additionalData);
  }

  public static ContextDTO get() {
    try {
      String json = MDC.get(CONTEXT_KEY);
      if (json == null) return null;

      return mapper.readValue(json, new TypeReference<ContextDTO>() {});
    } catch (JsonProcessingException ex) {
      log.error("#MDC - failed to get {}", CONTEXT_KEY, ex);
      return null;
    }
  }

  public static byte[] getAdditionalData() {
    String json = MDC.get(ADDITIONAL_DATA_KEY);
    if (json == null) return null;

    return json.getBytes(StandardCharsets.UTF_8);
  }

  public static void cleanUp() {
    MDC.clear();
  }
}
