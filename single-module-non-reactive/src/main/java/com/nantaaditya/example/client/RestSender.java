package com.nantaaditya.example.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

@Slf4j
public class RestSender {
  private final String name;
  private final RestClient restClient;
  private final RetryTemplate retryTemplate;

  private static final Set<HttpMethod> ELIGIBLE_METHOD_WITH_PAYLOAD
      = Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE);

  private RestSender(Builder builder) {
    this.name = builder.name;
    this.restClient = builder.restClient;
    this.retryTemplate = builder.retryTemplate;
  }

  public <S, T> ResponseEntity<T> executeWithRetry(HttpMethod httpMethod, String apiPath,
      MultiValueMap<String, String> queryParams, HttpHeaders headers, S request,
      Class<T> responseType, String processName) {

    if (retryTemplate == null) {
      throw new IllegalArgumentException(String.format("#Client - [%s] retryTemplate not set", this.name));
    }

    return retryTemplate.execute(context -> call(httpMethod, apiPath, queryParams, headers, request,
        responseType, context, processName));
  }

  public <S, T> ResponseEntity<T> execute(HttpMethod httpMethod, String apiPath,
      MultiValueMap<String, String> queryParams, HttpHeaders headers, S request,
      Class<T> responseType) {

    return call(httpMethod, apiPath, queryParams, headers, request, responseType,
        null, null);
  }

  private <S, T> ResponseEntity<T> call(HttpMethod httpMethod, String apiPath,
      MultiValueMap<String, String> queryParams, HttpHeaders headers, S request,
      Class<T> responseType, RetryContext retryContext, String processName) {
    try {
      RequestBodySpec requestBodySpec = restClient
          .method(httpMethod)
          .uri(builder -> {
            UriBuilder uriBuilder = builder.path(apiPath);
            if (queryParams != null) {
              uriBuilder = uriBuilder.queryParams(queryParams);
            }
            return uriBuilder.build();
          })
          .headers(h -> h.putAll(headers));

      if (ELIGIBLE_METHOD_WITH_PAYLOAD.contains(httpMethod)) {
        requestBodySpec = requestBodySpec.body(request);
      }

      return requestBodySpec
          .retrieve()
          .toEntity(responseType);
    } catch (Throwable ex) {
      log.error("#Client - [{}] has error, ", this.name, ex);
      if (retryTemplate != null) setAttributeOnRetryContext(retryContext, request, processName, ex);
      throw ex;
    }
  }

  private <S> void setAttributeOnRetryContext(RetryContext context, S request, String processName,
      Throwable e) {
    context.setAttribute("request", request);
    context.setAttribute("exception", e.getCause());
    context.setAttribute("processType", "client");
    context.setAttribute("processName", processName);
    if (e instanceof RestClientResponseException ex) {
      context.setAttribute("response", ex.getResponseBodyAsString());
    }
  }

  public static class Builder {
    private String name;
    private RestClient restClient;
    private RetryTemplate retryTemplate;

    public Builder() {}

    public Builder(@NotBlank(message = "NotBlank") String name,
        @NotNull(message = "NotNull") RestClient restClient) {
      this.name = name;
      this.restClient = restClient;
    }

    public Builder name(@NotBlank(message = "NotBlank") String name) {
      this.name = name;
      return this;
    }

    public Builder restClient(@NotNull(message = "NotNull") RestClient restClient) {
      this.restClient = restClient;
      return this;
    }

    public Builder retryTemplate(@NotNull(message = "NotNull") RetryTemplate retryTemplate) {
      this.retryTemplate = retryTemplate;
      return this;
    }

    public RestSender build() {
      if (name == null || name.isBlank()) throw new IllegalArgumentException("#Client - name should not null / blank");
      if (restClient == null) throw new IllegalArgumentException("#Client - restClient should not null");

      return new RestSender(this);
    }
  }
}
