package com.nantaaditya.example.interceptor;

import com.google.gson.Gson;
import com.nantaaditya.example.entity.EventLog;
import com.nantaaditya.example.helper.ContextHelper;
import com.nantaaditya.example.helper.GsonHelper;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.model.dto.CacheBodyRequest;
import com.nantaaditya.example.model.dto.ContextDTO;
import com.nantaaditya.example.properties.LogProperties;
import com.nantaaditya.example.repository.EventLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.StreamUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Log4j2
public class EventLogInterceptor implements HandlerInterceptor {

  private final EventLogRepository eventLogRepository;
  private final LogProperties logProperties;
  private final Gson gson;

  public EventLogInterceptor(EventLogRepository eventLogRepository, LogProperties logProperties,
      Gson gson) {
    this.eventLogRepository = eventLogRepository;
    this.logProperties = logProperties;
    this.gson = gson;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) throws Exception {
    try {
      ContextDTO context = ContextHelper.get();
      byte[] additionalData = ContextHelper.getAdditionalData();

      if (context == null) {
        log.warn(AppLogMessage.create("#EventLog - context is null"));
        return;
      }

      if (logProperties.isIgnoredTraceLogPath(context.path())) {
        log.debug(AppLogMessage.create("#EventLog - ignored trace log path"));
        return;
      }

      CacheBodyRequest cacheBodyRequest = new CacheBodyRequest(request);
      InputStream inputStream = cacheBodyRequest.getInputStream();
      byte[] body = StreamUtils.copyToByteArray(inputStream);
      String cleanBody = GsonHelper.cleanJson(new String(body), gson);

      EventLog eventLog = EventLog.builder()
          .clientId(context.clientId())
          .requestId(context.requestId())
          .method(context.method())
          .path(context.path())
          .responseCode(context.responseCode())
          .responseDescription(context.responseDescription())
          .payload(cleanBody.getBytes(StandardCharsets.UTF_8))
          .additionalData(additionalData)
          .createdDate(LocalDateTime.now())
          .build();
      log.debug(AppLogMessage.create("#EventLog - save event log", eventLog));
      eventLogRepository.save(eventLog);
    } catch (Exception e) {
      log.error(AppLogMessage.create("#EventLog - failed save event log", e));
    }
  }
}
