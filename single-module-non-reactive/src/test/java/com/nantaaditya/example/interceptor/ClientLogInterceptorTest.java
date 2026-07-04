package com.nantaaditya.example.interceptor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.nantaaditya.example.helper.ObservationHelper;
import com.nantaaditya.example.model.constant.LogFormat;
import com.nantaaditya.example.properties.ClientProperties;
import com.nantaaditya.example.properties.LogProperties;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@ExtendWith(MockitoExtension.class)
class ClientLogInterceptorTest {

  private ClientLogInterceptor interceptor;

  private Gson gson = new Gson();
  @Mock
  private ObservationHelper observationHelper;
  @Mock
  private LogProperties logProperties;
  @Mock
  private HttpRequest httpRequest;
  @Mock
  private ClientHttpRequestExecution execution;
  @Mock
  private ClientHttpResponse httpResponse;
  @Mock
  private ClientProperties clientProperties;

  private static final String payload = """
      {"key": "valueeeee"}
      """;

  @BeforeEach
  void setUp() {
    when(logProperties.getSensitiveFields())
        .thenReturn(Set.of("key"));
    when(observationHelper.getObservationRegistry())
        .thenReturn(ObservationRegistry.NOOP);
  }

  @Test
  void intercept_http() throws URISyntaxException, IOException {
    when(logProperties.logFormat())
        .thenReturn(LogFormat.TEXT);
    interceptor = new ClientLogInterceptor(observationHelper, gson, logProperties);
    when(httpRequest.getMethod())
        .thenReturn(HttpMethod.GET);
    when(httpRequest.getURI())
        .thenReturn(new URI("http://google.com"));

    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", "other");
    when(httpRequest.getHeaders())
        .thenReturn(new HttpHeaders(map));

    when(httpResponse.getStatusCode())
        .thenReturn(HttpStatusCode.valueOf(200));
    when(httpResponse.getHeaders())
        .thenReturn(new HttpHeaders(map));
    when(execution.execute(httpRequest, payload.getBytes(StandardCharsets.UTF_8)))
        .thenReturn(httpResponse);

    assertNotNull(interceptor.intercept(httpRequest, payload.getBytes(StandardCharsets.UTF_8), execution));
  }

  @Test
  void intercept_json() throws URISyntaxException, IOException {
    when(logProperties.logFormat())
        .thenReturn(LogFormat.JSON);
    interceptor = new ClientLogInterceptor(observationHelper, gson, logProperties);
    when(httpRequest.getMethod())
        .thenReturn(HttpMethod.GET);
    when(httpRequest.getURI())
        .thenReturn(new URI("http://google.com"));

    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", "other");
    when(httpRequest.getHeaders())
        .thenReturn(new HttpHeaders(map));

    when(httpResponse.getStatusCode())
        .thenReturn(HttpStatusCode.valueOf(200));
    when(httpResponse.getHeaders())
        .thenReturn(new HttpHeaders(map));
    when(execution.execute(httpRequest, payload.getBytes(StandardCharsets.UTF_8)))
        .thenReturn(httpResponse);

    assertNotNull(interceptor.intercept(httpRequest, payload.getBytes(StandardCharsets.UTF_8), execution));
  }

  @Test
  void intercept_errorStatus_decoratesResponseCodeWithoutMarkingError() throws URISyntaxException, IOException {
    when(logProperties.logFormat())
        .thenReturn(LogFormat.TEXT);
    interceptor = new ClientLogInterceptor(observationHelper, gson, logProperties);
    when(httpRequest.getMethod())
        .thenReturn(HttpMethod.GET);
    when(httpRequest.getURI())
        .thenReturn(new URI("http://google.com"));

    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", "other");
    when(httpRequest.getHeaders())
        .thenReturn(new HttpHeaders(map));

    when(httpResponse.getStatusCode())
        .thenReturn(HttpStatusCode.valueOf(500));
    when(httpResponse.getHeaders())
        .thenReturn(new HttpHeaders(map));
    when(execution.execute(httpRequest, payload.getBytes(StandardCharsets.UTF_8)))
        .thenReturn(httpResponse);

    assertNotNull(interceptor.intercept(httpRequest, payload.getBytes(StandardCharsets.UTF_8), execution));

    verify(observationHelper).decorateResponseObservation(any(Observation.class), eq("500"));
    verify(observationHelper, never())
        .decorateResponseObservation(any(Observation.class), any(Throwable.class), any(String.class));
  }

  @Test
  void intercept_executionThrows_decoratesErrorAndRethrows() throws URISyntaxException, IOException {
    when(logProperties.logFormat())
        .thenReturn(LogFormat.TEXT);
    interceptor = new ClientLogInterceptor(observationHelper, gson, logProperties);
    when(httpRequest.getMethod())
        .thenReturn(HttpMethod.GET);
    when(httpRequest.getURI())
        .thenReturn(new URI("http://google.com"));

    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", "other");
    when(httpRequest.getHeaders())
        .thenReturn(new HttpHeaders(map));

    IOException executionFailure = new IOException("connection refused");
    when(execution.execute(httpRequest, payload.getBytes(StandardCharsets.UTF_8)))
        .thenThrow(executionFailure);

    IOException thrown = assertThrows(IOException.class,
        () -> interceptor.intercept(httpRequest, payload.getBytes(StandardCharsets.UTF_8), execution));

    assertSame(executionFailure, thrown);
    verify(observationHelper)
        .decorateResponseObservation(any(Observation.class), eq(executionFailure), ArgumentMatchers.<String>isNull());
  }
}
