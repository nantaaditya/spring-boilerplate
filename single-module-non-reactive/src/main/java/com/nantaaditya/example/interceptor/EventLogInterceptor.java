package com.nantaaditya.example.interceptor;

import com.nantaaditya.example.entity.EventLog;
import com.nantaaditya.example.helper.ContextHelper;
import com.nantaaditya.example.model.dto.ContextDTO;
import com.nantaaditya.example.repository.EventLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class EventLogInterceptor implements HandlerInterceptor {

  private final EventLogRepository eventLogRepository;

  public EventLogInterceptor(EventLogRepository eventLogRepository) {
    this.eventLogRepository = eventLogRepository;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) throws Exception {
    try {
      ContextDTO context = ContextHelper.get();
      byte[] additionalData = ContextHelper.getAdditionalData();

      if (context == null) {
        log.warn("#EventLog - context is null");
        return;
      }

      EventLog eventLog = EventLog.builder()
          .clientId(context.clientId())
          .requestId(context.requestId())
          .method(context.method())
          .path(context.path())
          .responseCode(context.responseCode())
          .responseDescription(context.responseDescription())
          .additionalData(additionalData)
          .createdDate(LocalDateTime.now())
          .build();
      log.debug("#EventLog - save event log {}", eventLog);
      eventLogRepository.save(eventLog);
    } catch (Exception e) {
      log.error("#EventLog - failed save event log, ", e);
    }
  }
}
