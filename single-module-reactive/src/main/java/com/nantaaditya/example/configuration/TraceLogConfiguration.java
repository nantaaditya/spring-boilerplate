package com.nantaaditya.example.configuration;


import com.nantaaditya.example.helper.ContextHelper;
import com.nantaaditya.example.helper.MaskingHelper;
import com.nantaaditya.example.helper.TracerHelper;
import com.nantaaditya.example.model.constant.HeaderConstant;
import com.nantaaditya.example.properties.LogProperties;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TraceLogConfiguration implements HttpExchangeRepository {

  private final LogProperties logProperties;
  private final AtomicReference<HttpExchange> httpTrace = new AtomicReference<>();
  private final TracerHelper tracerHelper;
  private final ContextHelper contextHelper;

  private static final String BREAKPOINT = "\n";

  @Override
  public List<HttpExchange> findAll() {
    return Collections.singletonList(httpTrace.get());
  }

  @Override
  public void add(HttpExchange trace) {
    HttpExchange.Request request = trace.getRequest();
    HttpExchange.Response response = trace.getResponse();
    String requestId = tracerHelper.getBaggage(HeaderConstant.REQUEST_ID);

    if (!logProperties.enableTraceLog()) {
      contextHelper.cleanUp(requestId);
      return;
    }

    if (logProperties.isIgnoredTraceLogPath(request.getUri().getPath())) {
      contextHelper.cleanUp(requestId);
      return;
    }

    StringBuilder logContent = new StringBuilder("#Trace");
    logContent.append(BREAKPOINT);
    logContent
        .append(request.getMethod())
        .append(" ")
        .append((request.getUri().getRawQuery() != null ) ?
            request.getUri().getPath().concat("?").concat(request.getUri().getRawQuery())
            : request.getUri().getPath());
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
