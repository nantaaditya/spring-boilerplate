package com.nantaaditya.example.helper;

import com.nantaaditya.example.model.constant.ContextConstant;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.model.dto.ContextDTO;
import java.nio.charset.StandardCharsets;
import java.util.function.UnaryOperator;
import lombok.extern.log4j.Log4j2;
import org.slf4j.MDC;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Log4j2
public class ContextHelper {

  private static final ObjectMapper mapper = new ObjectMapper();

  private static final String CONTEXT_KEY = "context";
  private static final String ADDITIONAL_DATA_KEY = "additionalData";

  private ContextHelper() {}

  public static void put(ContextDTO contextDTO) {
    try {
      String json = mapper.writeValueAsString(contextDTO);
      MDC.put(ContextConstant.REQUEST_ID.getValue(), contextDTO.requestId());
      MDC.put(CONTEXT_KEY, json);
    } catch (JacksonException ex) {
      log.error(AppLogMessage.message("#MDC - failed to save {}", CONTEXT_KEY).error(ex));
    }
  }

  public static void update(UnaryOperator<ContextDTO> contextFunction) {
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
    } catch (JacksonException ex) {
      log.error(AppLogMessage.message("#MDC - failed to get {}", CONTEXT_KEY).error(ex));
      return null;
    }
  }

  public static String get(ContextConstant contextConstant) {
    ContextDTO contextDTO = get();
    if (contextDTO == null) return null;

    return switch (contextConstant) {
      case CLIENT_ID -> contextDTO.clientId();
      case REQUEST_ID -> contextDTO.requestId();
      case REQUEST_TIME -> contextDTO.requestTime();
      case RECEIVED_TIME -> contextDTO.receivedTime();
      case RESPONSE_CODE -> contextDTO.responseCode();
      case RESPONSE_DESCRIPTION -> contextDTO.responseDescription();
      case RESPONSE_TIME -> contextDTO.responseTime();
      default -> null;
    };
  }

  public static String getRequestId() {
    return get(ContextConstant.REQUEST_ID);
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
