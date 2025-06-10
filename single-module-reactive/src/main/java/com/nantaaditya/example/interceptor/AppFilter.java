package com.nantaaditya.example.interceptor;

import com.nantaaditya.example.helper.ContextHelper;
import com.nantaaditya.example.helper.DateTimeHelper;
import com.nantaaditya.example.helper.EventLogHelper;
import com.nantaaditya.example.model.constant.HeaderConstant;
import com.nantaaditya.example.model.dto.ContextDTO;
import io.micrometer.context.ContextRegistry;
import java.time.ZonedDateTime;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class AppFilter implements WebFilter {

  @Value("${spring.webflux.base-path}")
  private String contextPath;

  @Autowired
  private EventLogHelper eventLogHelper;

  @Autowired
  private ContextHelper contextHelper;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    ServerHttpResponse response = exchange.getResponse();
    ContextDTO contextDTO = decorateContext(request);

    contextHelper.put(contextDTO);
    decorateResponseHeaders(request, response);
    decorateMDC(contextDTO);

    return DataBufferUtils.join(request.getBody())
        .cast(DataBuffer.class)
        .switchIfEmpty(Mono.fromCallable(() ->
          exchange.getResponse().bufferFactory().allocateBuffer(0)
        ))
        .flatMap(dataBuffer -> {
          byte[] bodyBytes = new byte[dataBuffer.readableByteCount()];

          setCachedAttribute(exchange, dataBuffer, bodyBytes);
          Flux<DataBuffer> cachedBody = getDataBufferFlux(exchange, bodyBytes);
          ServerHttpRequest mutatedRequest = cachedServerHttpRequest(request, cachedBody);
          ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

          return chain.filter(mutatedExchange)
              .doFinally(signalType -> eventLogHelper.save(mutatedExchange));
        })
        .contextWrite(Context.of("context", contextDTO))
        .contextWrite(Context.of(HeaderConstant.CLIENT_ID.getHeader(), contextDTO.getClientId()))
        .contextWrite(Context.of(HeaderConstant.REQUEST_ID.getHeader(), contextDTO.getRequestId()));
  }

  private static void decorateMDC(ContextDTO contextDTO) {
    ContextRegistry.getInstance()
        .registerThreadLocalAccessor(
          HeaderConstant.CLIENT_ID.getHeader(),
          () -> MDC.get(HeaderConstant.CLIENT_ID.getHeader()),
    value -> MDC.put(HeaderConstant.CLIENT_ID.getHeader(), contextDTO.getClientId()),
          () -> MDC.remove(HeaderConstant.CLIENT_ID.getHeader()));

    ContextRegistry.getInstance()
        .registerThreadLocalAccessor(
            HeaderConstant.REQUEST_ID.getHeader(),
            () -> MDC.get(HeaderConstant.REQUEST_ID.getHeader()),
            value -> MDC.put(HeaderConstant.REQUEST_ID.getHeader(), contextDTO.getRequestId()),
            () -> MDC.remove(HeaderConstant.REQUEST_ID.getHeader()));
  }

  private void decorateResponseHeaders(ServerHttpRequest request, ServerHttpResponse response) {
    MultiValueMap<String, String> requestHeaders = request.getHeaders();
    String receivedTime = DateTimeHelper.getDateInFormat(ZonedDateTime.now(),
        DateTimeHelper.ISO_8601_GMT7_FORMAT);
    response.getHeaders().addAll(requestHeaders);
    response.getHeaders().add(HeaderConstant.RECEIVED_TIME.getHeader(), receivedTime);
  }

  private Flux<DataBuffer> getDataBufferFlux(ServerWebExchange exchange, byte[] bodyBytes) {
    return Flux.defer(() -> {
      DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bodyBytes);
      return Mono.just(buffer);
    });
  }

  private void setCachedAttribute(ServerWebExchange exchange, DataBuffer dataBuffer, byte[] bodyBytes) {
    if (dataBuffer.readableByteCount() > 0) {
      dataBuffer.read(bodyBytes);
    }
    DataBufferUtils.release(dataBuffer);
    exchange.getAttributes().put("cachedRequestBody", bodyBytes);
  }

  private ContextDTO decorateContext(ServerHttpRequest request) {
    ContextDTO contextDTO = new ContextDTO();
    contextDTO.decorateContext(request, contextPath);
    return contextDTO;
  }

  private ServerHttpRequest cachedServerHttpRequest(ServerHttpRequest request,
      Flux<DataBuffer> cachedBody) {
    return new ServerHttpRequestDecorator(request) {
      @Override
      public Flux<DataBuffer> getBody() {
        return cachedBody;
      }
    };
  }
}
