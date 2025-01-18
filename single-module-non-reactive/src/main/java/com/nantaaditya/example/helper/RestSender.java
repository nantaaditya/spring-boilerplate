package com.nantaaditya.example.helper;

import com.nantaaditya.example.model.constant.HeaderConstant;
import com.nantaaditya.example.model.constant.RetryConstant;
import com.nantaaditya.example.model.dto.ClientRequest;
import com.nantaaditya.example.model.dto.ContextDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class RestSender {
  private final String name;
  private final RestTemplate restClient;
  private final RetryTemplate retryTemplate;
  private final String clientId;

  private RestSender(Builder builder) {
    this.name = builder.name;
    this.restClient = builder.restClient;
    this.retryTemplate = builder.retryTemplate;
    this.clientId = builder.clientId;
  }

  public <S, T> ResponseEntity<T> executeWithRetry(HttpMethod httpMethod, String apiPath,
      HttpHeaders headers, S request, Class<T> responseType, String processName) {

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
      Class<T> responseType, String processName) {

    if (retryTemplate == null) {
      throw new IllegalArgumentException(String.format("#Client - [%s] retryTemplate not set", this.name));
    }

    return retryTemplate.execute(context -> call(
        new ClientRequest<>(httpMethod, apiPath, queryParams, headers, request,
            responseType, context, processName)
    ));
  }

  public <S, T> ResponseEntity<T> execute(HttpMethod httpMethod, String apiPath,
      HttpHeaders headers, S request, Class<T> responseType) {
    return call(new ClientRequest<>(httpMethod, apiPath, null, headers, request,
        responseType, null, null));
  }

  public <S, T> ResponseEntity<T> execute(HttpMethod httpMethod, String apiPath,
      MultiValueMap<String, String> queryParams, HttpHeaders headers, S request,
      Class<T> responseType) {


    return call(new ClientRequest<>(httpMethod, apiPath, queryParams, headers, request,
        responseType, null, null));
  }

  // Base Method
  private <S, T> ResponseEntity<T> call(ClientRequest<S, T> request) {
    try {
      StringBuilder pathBuilder = getPath(request);
      HttpEntity<S> httpEntity = request.request() == null ?
          new HttpEntity<>(composeHttpHeaders(request.headers()))
          : new HttpEntity<>(request.request(), composeHttpHeaders(request.headers()));

      return restClient.exchange(pathBuilder.toString(), request.method(), httpEntity, request.responseType());
    } catch (Throwable ex) {
      log.error("#Client - [{}] has error, ", this.name, ex);
      if (retryTemplate != null)
        setAttributeOnRetryContext(request.retryContext(), request.request(), request.processName(), ex);
      throw ex;
    }
  }

  private static <S, T> StringBuilder getPath(ClientRequest<S, T> request) {
    StringBuilder pathBuilder = new StringBuilder(request.path());
    if (request.queryParams() != null) {
      pathBuilder.append("?");

      MultiValueMap<String, String> queryParams = request.queryParams();
      for (Entry<String, List<String>> entry : queryParams.entrySet()) {
        pathBuilder.append(entry.getKey()).append("=").append(entry.getValue().get(0)).append("&");
      }

      pathBuilder.deleteCharAt(pathBuilder.length() - 1);
    }
    return pathBuilder;
  }

  private <S> void setAttributeOnRetryContext(RetryContext context, S request, String processName,
      Throwable e) {
    context.setAttribute(RetryConstant.REQUEST.getName(), request);
    context.setAttribute(RetryConstant.EXCEPTION.getName(), e.getCause());
    context.setAttribute(RetryConstant.PROCESS_TYPE.getName(), "client");
    context.setAttribute(RetryConstant.PROCESS_NAME.getName(), processName);
    if (e instanceof RestClientResponseException ex) {
      context.setAttribute(RetryConstant.EXCEPTION.getName(), ex.getResponseBodyAsString());
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

  public static class Builder {
    private final String name;
    private final String clientId;
    private final RestTemplate restClient;
    private RetryTemplate retryTemplate;

    public Builder(
        @NotBlank(message = "NotBlank") String clientId,
        @NotBlank(message = "NotBlank") String name,
        @NotNull(message = "NotNull") RestTemplate restClient) {
      this.clientId = clientId;
      this.name = name;
      this.restClient = restClient;
    }

    public Builder retryTemplate(@NotNull(message = "NotNull") RetryTemplate retryTemplate) {
      this.retryTemplate = retryTemplate;
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
