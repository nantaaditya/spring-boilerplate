package com.nantaaditya.example.configuration;


import com.nantaaditya.example.helper.ContextHelper;
import com.nantaaditya.example.helper.MaskingHelper;
import com.nantaaditya.example.model.constant.HeaderConstant;
import com.nantaaditya.example.properties.LogProperties;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.HttpExchange.Request;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TraceLogConfiguration implements HttpExchangeRepository {

  private final LogProperties logProperties;
  private final AtomicReference<HttpExchange> httpTrace = new AtomicReference<>();

  private static final String BREAKPOINT = "\n";

  @Override
  public List<HttpExchange> findAll() {
    return Collections.singletonList(httpTrace.get());
  }

  @Override
  public void add(HttpExchange trace) {
    HttpExchange.Request request = trace.getRequest();

    if (!logProperties.enableTraceLog()) {
      ContextHelper.cleanUp();
      return;
    }

    if (logProperties.isIgnoredTraceLogPath(request.getUri().getPath())) {
      ContextHelper.cleanUp();
      return;
    }

    switch (logProperties.logFormat()) {
      case TEXT -> logText(request, trace);
      case JSON -> logJson(request, trace);
      default -> {
        // do nothing
      }
    }

    ContextHelper.cleanUp();
  }

  private void logText(HttpExchange.Request request, HttpExchange trace) {
    HttpExchange.Response response = trace.getResponse();

    StringBuilder logContent = new StringBuilder("#Trace");
    logContent.append(BREAKPOINT);
    logContent
        .append(request.getMethod())
        .append(" ")
        .append(getURI(request));
    logContent.append(BREAKPOINT);
    logContent
        .append("http status: [")
        .append(response.getStatus())
        .append("] ")
        .append("time taken: [")
        .append(trace.getTimeTaken().toMillis())
        .append("] ms")
        .append(BREAKPOINT);

    for (Entry<String, List<String>> headers : response.getHeaders().entrySet()) {
      if (isInternalHeader(headers.getKey())) {
        maskHeader(logContent, headers);
      }
    }

    log.info(logContent.toString());
  }

  private static String getURI(Request request) {
    return (request.getUri().getRawQuery() != null) ?
        request.getUri().getPath().concat("?").concat(request.getUri().getRawQuery())
        : request.getUri().getPath();
  }

  private void logJson(HttpExchange.Request request, HttpExchange trace) {
    HttpExchange.Response response = trace.getResponse();

    Map<String, Object> content = new LinkedHashMap<>();
    content.put("method", request.getMethod());
    content.put("uri", getURI(request));
    content.put("http status", "[" + response.getStatus() + "]");
    content.put("time taken", "[" + trace.getTimeTaken().toMillis() + "] ms");

    for (Entry<String, List<String>> headers : response.getHeaders().entrySet()) {
      if (isInternalHeader(headers.getKey())) {
        content.put("headers", logProperties.isSensitiveField(headers.getKey()) ?
            headers.getValue().stream().map(MaskingHelper::masking).toList()  : headers.getValue());
      }
    }
    log.info("{}", content);
  }

  private boolean isInternalHeader(String headerKey) {
    return Stream.of(HeaderConstant.values())
        .anyMatch(h -> h.getHeader().equals(headerKey));
  }

  private void maskHeader(StringBuilder logContent, Entry<String, List<String>> headers) {
    logContent
        .append(headers.getKey())
        .append(": ")
        .append(logProperties.isSensitiveField(headers.getKey()) ?
            headers.getValue().stream().map(MaskingHelper::masking).toList()  : headers.getValue()
        )
        .append(BREAKPOINT);
  }
}
