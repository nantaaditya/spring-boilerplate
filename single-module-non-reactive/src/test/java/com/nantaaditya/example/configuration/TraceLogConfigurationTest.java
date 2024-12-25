package com.nantaaditya.example.configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nantaaditya.example.properties.LogProperties;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.HttpExchange.Request;

@ExtendWith(MockitoExtension.class)
class TraceLogConfigurationTest {

  @InjectMocks
  private TraceLogConfiguration traceLogConfiguration;

  @Mock
  private LogProperties logProperties;

  @Test
  void add_traceLogDisable() {
    HttpExchange httpExchange = new HttpExchange(
        Instant.now(),
        null,
        null,
        null,
        null,
        Duration.ofSeconds(1)
    );

    when(logProperties.enableTraceLog()).thenReturn(false);
    traceLogConfiguration.add(httpExchange);
    verify(logProperties).enableTraceLog();
  }

  @Test
  void add_ignoreTraceLogPath() throws URISyntaxException {
    HttpExchange httpExchange = new HttpExchange(
        Instant.now(),
        new Request(
            new URI("http://localhost:8080/actuator/health"),
            "http://localhost:8080",
            "GET",
            Collections.emptyMap()
        ),
        null,
        null,
        null,
        Duration.ofSeconds(1)
    );

    when(logProperties.enableTraceLog()).thenReturn(true);
    when(logProperties.isIgnoredTraceLogPath(anyString())).thenReturn(true);
    traceLogConfiguration.add(httpExchange);
    verify(logProperties).enableTraceLog();
    verify(logProperties).isIgnoredTraceLogPath(anyString());
  }

  @Test
  void findAll() {
    assertNotNull(traceLogConfiguration.findAll());
  }
}