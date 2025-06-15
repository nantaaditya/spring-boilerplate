package com.nantaaditya.example.helper;

import com.google.gson.Gson;
import com.nantaaditya.example.entity.EventLog;
import com.nantaaditya.example.model.constant.HeaderConstant;
import com.nantaaditya.example.model.dto.ContextDTO;
import com.nantaaditya.example.properties.LogProperties;
import com.nantaaditya.example.repository.EventLogRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventLogHelper {

  private final EventLogRepository eventLogRepository;
  private final LogProperties logProperties;
  private final Gson gson;
  private final ContextHelper contextHelper;
  private final TracerHelper tracerHelper;

  public void save(ServerWebExchange exchange) {
    try {
      String requestId = tracerHelper.getBaggage(HeaderConstant.REQUEST_ID);
      if (requestId == null) {
        log.warn("#EventLog - requestId is null");
        return;
      }

      ContextDTO context = contextHelper.get(requestId);
      if (context == null) {
        log.warn("#EventLog - context is null");
        return;
      }

      byte[] additionalData = contextHelper.getAdditionalData(requestId);

      if (logProperties.isIgnoredTraceLogPath(context.getPath())) {
        log.debug("#EventLog - ignored trace log path");
        return;
      }

      byte[] cachedBody = (byte[]) exchange.getAttribute("cachedRequestBody");
      String payload = new String(cachedBody, StandardCharsets.UTF_8);
      String cleanedPayload = GsonHelper.cleanJson(payload, gson);

      Mono.fromSupplier(() -> createEventLog(context, additionalData, cleanedPayload))
          .flatMap(eventLogRepository::save)
          .subscribe(
              success -> log.debug("#EventLog - success save event log"),
              error -> log.error("#EventLog - error save event log {}, cause {}",
                  error.getMessage(), ErrorHelper.getRootCause(error)),
              () -> contextHelper.cleanUp(requestId)
          );
    } catch (Exception e) {
      log.error("#EventLog - failed save event log {}, cause {}", e.getMessage(), ErrorHelper.getRootCause(e));
    }
  }

  private EventLog createEventLog(ContextDTO context, byte[] additionalData, String payload) {
    return EventLog.builder()
        .id(TsidHelper.generateStringId())
        .clientId(context.getClientId())
        .requestId(context.getRequestId())
        .method(context.getMethod())
        .path(context.getPath())
        .responseCode(context.getResponseCode())
        .responseDescription(context.getResponseDescription())
        .payload(payload.getBytes(StandardCharsets.UTF_8))
        .additionalData(additionalData)
        .createdDate(LocalDateTime.now())
        .build();
  }
}
