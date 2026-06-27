package com.nantaaditya.example.helper;

import com.nantaaditya.example.model.constant.HeaderConstant;
import com.nantaaditya.example.model.constant.RetryConstant;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.model.dto.ClientRequest;
import com.nantaaditya.example.model.dto.ContextDTO;
import com.nantaaditya.example.model.observation.ObservableRetryable;
import com.nantaaditya.example.properties.embedded.RetryConfiguration;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Log4j2
public class RestSender {
  private final String name;
  private final RestClient restClient;
  private final RetryTemplate retryTemplate;
  private final RetryConfiguration retryConfiguration;
  private final ObjectMapper objectMapper;
  private final String clientId;


  private RestSender(Builder builder) {
    this.name = builder.name;
    this.restClient = builder.restClient;
    this.retryTemplate = builder.retryTemplate;
    this.retryConfiguration = builder.retryConfiguration;
    this.objectMapper = builder.objectMapper;
    this.clientId = builder.clientId;
  }

  public <S, T> ResponseEntity<T> executeWithRetry(HttpMethod httpMethod, String apiPath,
      HttpHeaders headers, S request, ParameterizedTypeReference<T> responseType, String processName) {

    return executeWithRetry(httpMethod, apiPath, null, headers, request, responseType, processName);
  }

  @SneakyThrows
  public <S, T> ResponseEntity<T> executeWithRetry(HttpMethod httpMethod, String apiPath,
      MultiValueMap<String, String> queryParams, HttpHeaders headers, S request,
      ParameterizedTypeReference<T> responseType, String processName) {

    if (retryTemplate == null) {
      throw new IllegalArgumentException(String.format("#Client - [%s] retryTemplate not set", this.name));
    }

    Map<String, Object> retryContext = new ConcurrentHashMap<>();
    ObservableRetryable<ResponseEntity<T>> wrapper = new ObservableRetryable<>(
        () -> call(new ClientRequest<>(httpMethod, apiPath, queryParams, headers, request,
            responseType, retryContext, processName)),
        retryContext
    );
    try {
      return retryTemplate.execute(wrapper);
    } catch (RetryException e) {
      throw e.getCause() != null ? e.getCause() : e;
    }
  }

  public <S, T> ResponseEntity<T> execute(HttpMethod httpMethod, String apiPath,
      HttpHeaders headers, S request, ParameterizedTypeReference<T> responseType) {
    return execute(httpMethod, apiPath, null, headers, request, responseType);
  }

  public <S, T> ResponseEntity<T> execute(HttpMethod httpMethod, String apiPath,
      MultiValueMap<String, String> queryParams, HttpHeaders headers, S request,
      ParameterizedTypeReference<T> responseType) {


    return call(new ClientRequest<>(httpMethod, apiPath, queryParams, headers, request,
        responseType, null, null));
  }

  // Base Method
  @SneakyThrows
  private <S, T> ResponseEntity<T> call(ClientRequest<S, T> request) {

    HttpEntity<S> httpEntity = request.request() == null ?
        new HttpEntity<>(composeHttpHeaders(request.headers()))
        : new HttpEntity<>(request.request(), composeHttpHeaders(request.headers()));

    try {
      RequestBodySpec spec = restClient
          .method(request.method())
          .uri(builder -> composeUri(request, builder))
          .headers(headers -> headers.addAll(composeHttpHeaders(request.headers())));

      if (request.request() != null) {
        spec = spec.body(request.request());
      }

      return spec.exchange((HttpRequest httpRequest, ConvertibleClientHttpResponse response) -> {
        boolean success = response.getStatusCode().is2xxSuccessful();
        if (!success) {
          log.error(AppLogMessage.message("#Client - [{}] has http status {}", this.name, response.getStatusCode().value()));
        }

        if (response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is4xxClientError()) {
          T body = response.bodyTo(request.responseType());

          return ResponseEntity.status(response.getStatusCode())
              .body(body);
        }
        throw response.createException();
      });

    } catch (Throwable ex) {
      log.error(AppLogMessage.message("#Client - [{}] has error {}", this.name, ex.getMessage()).error(ex));
      if (retryTemplate != null && request.retryContext() != null) {
        setAttributeOnRetryContext(request, httpEntity, ex);
      }
      throw ex;
    }
  }

  private static <S, T> @NonNull URI composeUri(ClientRequest<S, T> request, UriBuilder builder) {
    builder = builder.path(request.path());
    if (request.queryParams() != null) {
      builder = builder.queryParams(request.queryParams());
    }
    return builder.build();
  }

  private <S, T> void setAttributeOnRetryContext(ClientRequest<S, T> clientRequest,
      HttpEntity<S> httpEntity, Throwable e) {
    clientRequest.retryContext().put(RetryConstant.CLIENT_NAME.getName(), name);
    clientRequest.retryContext().put(RetryConstant.METHOD.getName(), clientRequest.method());
    clientRequest.retryContext().put(RetryConstant.PATH.getName(), clientRequest.getFullPath());
    clientRequest.retryContext().put(RetryConstant.HEADERS.getName(), getHttpHeaderJson(httpEntity.getHeaders()));
    clientRequest.retryContext().put(RetryConstant.REQUEST.getName(), clientRequest.request());
    clientRequest.retryContext().put(RetryConstant.REQUEST_ID.getName(), httpEntity.getHeaders().getFirst(HeaderConstant.REQUEST_ID.getHeader()));
    clientRequest.retryContext().put(RetryConstant.EXCEPTION.getName(), e.getCause());
    clientRequest.retryContext().put(RetryConstant.PROCESS_TYPE.getName(), "client");
    clientRequest.retryContext().put(RetryConstant.PROCESS_NAME.getName(), clientRequest.processName());
    clientRequest.retryContext().put(RetryConstant.MAX_RETRY.getName(), Optional.ofNullable(retryConfiguration)
        .map(RetryConfiguration::maxAttempt)
        .orElse(1)
    );
    if (e instanceof RestClientResponseException ex) {
      clientRequest.retryContext().put(RetryConstant.RESPONSE.getName(), ex.getResponseBodyAsString());
    }
  }

  private HttpHeaders composeHttpHeaders(HttpHeaders requestHeaders) {
    HttpHeaders headers = new HttpHeaders();
    headers.putAll(requestHeaders);

    ContextDTO contextDTO = ContextHelper.get();
    List<String> requestId = Optional.ofNullable(contextDTO)
        .map(ContextDTO::requestId)
        .map(List::of)
        .orElseGet(() -> List.of(TsidHelper.generateTsid()));
    List<String> requestTime = List.of(DateTimeHelper.getDateInFormat(ZonedDateTime.now(),
        DateTimeHelper.ISO_8601_GMT7_FORMAT));

    headers.put(HeaderConstant.CLIENT_ID.getHeader(), List.of(clientId));
    headers.put(HeaderConstant.REQUEST_ID.getHeader(), requestId);
    headers.put(HeaderConstant.REQUEST_TIME.getHeader(), requestTime);

    return headers;
  }

  private String getHttpHeaderJson(HttpHeaders httpHeaders) {
    try {
      MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
      httpHeaders.forEach(headers::put);
      return objectMapper.writeValueAsString(headers);
    } catch (JacksonException e) {
      log.error(AppLogMessage.message("#Client - [{}] error convert header to json", name).error(e));
      return null;
    }
  }

  public static class Builder {
    private final String name;
    private final String clientId;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private RetryTemplate retryTemplate;
    private RetryConfiguration retryConfiguration;

    public Builder(
        @NotBlank(message = "NotBlank") String clientId,
        @NotBlank(message = "NotBlank") String name,
        @NotNull(message = "NotNull") RestClient restClient,
        @NotNull(message = "NotNull") ObjectMapper objectMapper) {
      this.clientId = clientId;
      this.name = name;
      this.restClient = restClient;
      this.objectMapper = objectMapper;
    }

    public Builder retryTemplate(RetryTemplate retryTemplate) {
      Optional.ofNullable(retryTemplate)
          .ifPresent(r -> this.retryTemplate = r);
      return this;
    }

    public Builder retryConfiguration(RetryConfiguration retryConfiguration) {
      Optional.ofNullable(retryConfiguration)
          .ifPresent(r -> this.retryConfiguration = r);
      return this;
    }

    public RestSender build() {
      if (name == null || name.isBlank())
        throw new IllegalArgumentException("#Client - name should not null / blank");
      if (restClient == null)
        throw new IllegalArgumentException("#Client - restClient should not null");

      return new RestSender(this);
    }
  }
}
