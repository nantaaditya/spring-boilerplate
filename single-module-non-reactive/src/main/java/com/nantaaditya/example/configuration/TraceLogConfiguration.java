package com.nantaaditya.example.configuration;


import com.nantaaditya.example.helper.ContextHelper;
import com.nantaaditya.example.helper.MaskingHelper;
import com.nantaaditya.example.model.constant.HeaderConstant;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.model.dto.JsonLogHttpResponse;
import com.nantaaditya.example.properties.LogProperties;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.HttpExchange.Request;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Log4j2
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

    log.info(AppLogMessage.create(logContent.toString()));
  }

  private static String getURI(Request request) {
    return (request.getUri().getRawQuery() != null) ?
        request.getUri().getPath().concat("?").concat(request.getUri().getRawQuery())
        : request.getUri().getPath();
  }

  private void logJson(HttpExchange.Request request, HttpExchange trace) {
    HttpExchange.Response response = trace.getResponse();

    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    for (Entry<String, List<String>> h : response.getHeaders().entrySet()) {
      if (isInternalHeader(h.getKey())) {
        headers.put(
            h.getKey(),
            logProperties.isSensitiveField(h.getKey()) ?
              h.getValue().stream().map(MaskingHelper::masking).toList()  : h.getValue()
        );
      }
    }

    JsonLogHttpResponse content = new JsonLogHttpResponse(
        request.getMethod(),
        getURI(request),
        String.format("%s", response.getStatus()),
        String.format("%s ms", trace.getTimeTaken().toMillis()),
        headers,
        null
    );

    log.info(AppLogMessage.create("#Trace", content));
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
