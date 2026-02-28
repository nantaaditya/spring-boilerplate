package com.nantaaditya.example.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nantaaditya.example.model.constant.HeaderConstant;
import com.nantaaditya.example.model.constant.RetryConstant;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.model.dto.ClientRequest;
import com.nantaaditya.example.model.dto.ContextDTO;
import com.nantaaditya.example.properties.embedded.RetryConfiguration;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Log4j2
public class RestSender {
  private final String name;
  private final RestTemplate restClient;
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

    if (retryTemplate == null) {
      throw new IllegalArgumentException(String.format("#Client - [%s] retryTemplate not set", this.name));
    }

    return retryTemplate.execute(context -> call(
        new ClientRequest<>(httpMethod, apiPath, null, headers, request,
            responseType, context, processName)
    ));
  }

  public <S, T> ResponseEntity<T> executeWithRetry(HttpMethod httpMethod, String apiPath,
      MultiValueMap<String, String> queryParams, HttpHeaders headers, S request,
      ParameterizedTypeReference<T> responseType, String processName) {

    if (retryTemplate == null) {
      throw new IllegalArgumentException(String.format("#Client - [%s] retryTemplate not set", this.name));
    }

    return retryTemplate.execute(context -> call(
        new ClientRequest<>(httpMethod, apiPath, queryParams, headers, request,
            responseType, context, processName)
    ));
  }

  public <S, T> ResponseEntity<T> execute(HttpMethod httpMethod, String apiPath,
      HttpHeaders headers, S request, ParameterizedTypeReference<T> responseType) {
    return call(new ClientRequest<>(httpMethod, apiPath, null, headers, request,
        responseType, null, null));
  }

  public <S, T> ResponseEntity<T> execute(HttpMethod httpMethod, String apiPath,
      MultiValueMap<String, String> queryParams, HttpHeaders headers, S request,
      ParameterizedTypeReference<T> responseType) {


    return call(new ClientRequest<>(httpMethod, apiPath, queryParams, headers, request,
        responseType, null, null));
  }

  // Base Method
  private <S, T> ResponseEntity<T> call(ClientRequest<S, T> request) {

    HttpEntity<S> httpEntity = request.request() == null ?
        new HttpEntity<>(composeHttpHeaders(request.headers()))
        : new HttpEntity<>(request.request(), composeHttpHeaders(request.headers()));

    try {
      return restClient.exchange(request.getFullPath(), request.method(), httpEntity, request.responseType());
    } catch (Throwable ex) {
      log.error(AppLogMessage.message("#Client - [{}] has error, ", this.name).error(ex));
      if (retryTemplate != null)
        setAttributeOnRetryContext(request.retryContext(), request, httpEntity, ex);
      throw ex;
    }
  }

  private <S, T> void setAttributeOnRetryContext(RetryContext context, ClientRequest<S, T> clientRequest,
      HttpEntity<S> httpEntity, Throwable e) {
    context.setAttribute(RetryConstant.CLIENT_NAME.getName(), name);
    context.setAttribute(RetryConstant.METHOD.getName(), clientRequest.method());
    context.setAttribute(RetryConstant.PATH.getName(), clientRequest.getFullPath());
    context.setAttribute(RetryConstant.HEADERS.getName(), getHttpHeaderJson(httpEntity.getHeaders()));
    context.setAttribute(RetryConstant.REQUEST.getName(), clientRequest.request());
    context.setAttribute(RetryConstant.REQUEST_ID.getName(), httpEntity.getHeaders().getFirst(HeaderConstant.REQUEST_ID.getHeader()));
    context.setAttribute(RetryConstant.EXCEPTION.getName(), e.getCause());
    context.setAttribute(RetryConstant.PROCESS_TYPE.getName(), "client");
    context.setAttribute(RetryConstant.PROCESS_NAME.getName(), clientRequest.processName());
    context.setAttribute(RetryConstant.MAX_RETRY.getName(), Optional.ofNullable(retryConfiguration)
        .map(RetryConfiguration::maxAttempt)
        .orElse(1)
    );
    context.setAttribute(RetryConstant.EXCEPTION.getName(), e.getMessage());
    if (e instanceof RestClientResponseException ex) {
      context.setAttribute(RetryConstant.RESPONSE.getName(), ex.getResponseBodyAsString());
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
      MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
      headers.putAll(httpHeaders);
      return objectMapper.writeValueAsString(headers);
    } catch (JsonProcessingException e) {
      log.error(AppLogMessage.message("#Client - [{}] error convert header to json", name).error(e));
      return null;
    }
  }

  public static class Builder {
    private final String name;
    private final String clientId;
    private final RestTemplate restClient;
    private final ObjectMapper objectMapper;
    private RetryTemplate retryTemplate;
    private RetryConfiguration retryConfiguration;

    public Builder(
        @NotBlank(message = "NotBlank") String clientId,
        @NotBlank(message = "NotBlank") String name,
        @NotNull(message = "NotNull") RestTemplate restClient,
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
