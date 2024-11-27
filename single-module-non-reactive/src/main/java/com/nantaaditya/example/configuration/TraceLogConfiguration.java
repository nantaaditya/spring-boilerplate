package com.nantaaditya.example.configuration;


import com.nantaaditya.example.helper.ContextHelper;
import com.nantaaditya.example.model.constant.HeaderConstant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TraceLogConfiguration implements HttpExchangeRepository {

  AtomicReference<HttpExchange> httpTrace = new AtomicReference<>();

  @Override
  public List<HttpExchange> findAll() {
    return Collections.singletonList(httpTrace.get());
  }

  @Override
  public void add(HttpExchange trace) {
    HttpExchange.Request request = trace.getRequest();
    HttpExchange.Response response = trace.getResponse();

    log.trace("#TRACE: headers - {}", getHeaders(response.getHeaders()));
    log.trace("#TRACE: [{}] - {}, response {}, response time {}ms",
        request.getMethod(), request.getUri().toString(), response.getStatus(), trace.getTimeTaken());
    httpTrace.set(trace);

    ContextHelper.cleanUp();
  }

  private String getHeaders(Map<String, List<String>> headers) {
    StringBuilder sb = new StringBuilder();
    headers.entrySet()
        .stream()
        .filter(entry -> HeaderConstant.contains(entry.getKey()))
        .forEach(item -> sb.append("{").append(item.getKey()).append(": ").append(item.getValue()).append("},"));
    return sb.toString();
  }
}
