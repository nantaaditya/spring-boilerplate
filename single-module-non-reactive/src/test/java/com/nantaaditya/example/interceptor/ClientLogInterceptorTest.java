package com.nantaaditya.example.interceptor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.nantaaditya.example.properties.LogProperties;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
  private LogProperties logProperties;
  @Mock
  private HttpRequest httpRequest;
  @Mock
  private ClientHttpRequestExecution execution;
  @Mock
  private ClientHttpResponse httpResponse;

  private static final String payload = """
      {"key": "valueeeee"}
      """;

  @BeforeEach
  void setUp() {
    when(logProperties.getSensitiveFields())
        .thenReturn(Set.of("key"));
    interceptor = new ClientLogInterceptor(gson, logProperties);
  }

  @Test
  void intercept() throws URISyntaxException, IOException {
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
}
