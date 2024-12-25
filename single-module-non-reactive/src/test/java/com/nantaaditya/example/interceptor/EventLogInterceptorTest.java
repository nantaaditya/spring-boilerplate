package com.nantaaditya.example.interceptor;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nantaaditya.example.helper.ContextHelper;
import com.nantaaditya.example.model.dto.ContextDTO;
import com.nantaaditya.example.properties.LogProperties;
import com.nantaaditya.example.repository.EventLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class EventLogInterceptorTest {

  @Mock
  private LogProperties logProperties;

  @Mock
  private EventLogRepository eventLogRepository;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Test
  void afterCompletion_null() throws Exception {
    EventLogInterceptor interceptor = new EventLogInterceptor(eventLogRepository, logProperties);

    interceptor.afterCompletion(request, response, null, null);
  }

  @Test
  void afterCompletion_ignoredPath() throws Exception {
    ContextHelper.put(new ContextDTO(
        "clientId",
        "requestId",
        "POST",
        "/api",
        null, null, null, null, null
    ));

    when(logProperties.isIgnoredTraceLogPath(anyString())).thenReturn(true);
    EventLogInterceptor interceptor = new EventLogInterceptor(eventLogRepository, logProperties);

    interceptor.afterCompletion(request, response, null, null);
    verify(logProperties).isIgnoredTraceLogPath(anyString());
    MDC.clear();
  }
}