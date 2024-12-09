package com.nantaaditya.example.configuration;


import com.nantaaditya.example.helper.ContextHelper;
import com.nantaaditya.example.helper.MaskingHelper;
import com.nantaaditya.example.model.constant.HeaderConstant;
import com.nantaaditya.example.properties.LogProperties;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TraceLogConfiguration implements HttpExchangeRepository {

  @Autowired
  private LogProperties logProperties;
  private AtomicReference<HttpExchange> httpTrace = new AtomicReference<>();

  private static final String BREAKPOINT = "\n";

  @Override
  public List<HttpExchange> findAll() {
    return Collections.singletonList(httpTrace.get());
  }

  @Override
  public void add(HttpExchange trace) {
    HttpExchange.Request request = trace.getRequest();
    HttpExchange.Response response = trace.getResponse();

    if (!logProperties.isEnableTraceLog() || logProperties.isIgnoredTraceLogPath(request.getUri().getPath())) {
      ContextHelper.cleanUp();
      return;
    }

    StringBuilder logContent = new StringBuilder("#Trace");
    logContent.append(BREAKPOINT);
    logContent
        .append(request.getMethod())
        .append(" ")
        .append(request.getUri().getPath());
    logContent.append(BREAKPOINT);
    logContent
        .append("http status: [")
        .append(response.getStatus())
        .append("] ")
        .append("time taken: [")
        .append(trace.getTimeTaken().toMillis())
        .append("] ms")
        .append(BREAKPOINT);

    for (Entry<String, List<String>> headers : request.getHeaders().entrySet()) {
      if (isInternalHeader(headers.getKey())) {
        maskHeader(logContent, headers);
      }
    }

    log.info(logContent.toString());
    httpTrace.set(trace);

    ContextHelper.cleanUp();
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

  private boolean isInternalHeader(String headerKey) {
    return Stream.of(HeaderConstant.values())
        .anyMatch(h -> h.getHeader().equals(headerKey));
  }
}
