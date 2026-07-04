package com.nantaaditya.example.helper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.nantaaditya.example.properties.LogProperties;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpHeaders;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Origin;
import org.zalando.logbook.Precorrelation;
import org.zalando.logbook.attributes.HttpAttributes;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class ApiLogbookFormatterTest {

  private ApiLogbookFormatter formatter;

  @Mock
  private Gson gson;

  @Mock
  private LogProperties logProperties;

  @Mock
  private HttpRequest request;

  @Mock
  private HttpResponse response;

  @Mock
  private Precorrelation precorrelation;

  @Mock
  private Correlation correlation;

  private final ObjectMapper objectMapper = JsonMapper.builder().build();

  @BeforeEach
  void setUp() {
    formatter = new ApiLogbookFormatter(objectMapper, gson, logProperties);
  }

  @Test
  void format_request_emptyHeadersAndBody_returnsJson() throws IOException {
    when(precorrelation.getId()).thenReturn("corr-1");
    when(request.getOrigin()).thenReturn(Origin.LOCAL);
    when(request.getProtocolVersion()).thenReturn("HTTP/1.1");
    when(request.getRemote()).thenReturn("127.0.0.1");
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestUri()).thenReturn("http://example.com/api");
    when(request.getHost()).thenReturn("example.com");
    when(request.getPath()).thenReturn("/api");
    when(request.getScheme()).thenReturn("http");
    when(request.getPort()).thenReturn(Optional.of(8080));
    when(request.getHeaders()).thenReturn(HttpHeaders.empty());
    when(request.getAttributes()).thenReturn(HttpAttributes.EMPTY);
    when(request.getBodyAsString()).thenReturn("");
    when(request.getContentType()).thenReturn("application/json");

    String result = formatter.format(precorrelation, request);

    assertNotNull(result);
  }

  @Test
  void format_request_withAttributes_includesAttributes() throws IOException {
    when(precorrelation.getId()).thenReturn("corr-2");
    when(request.getOrigin()).thenReturn(Origin.REMOTE);
    when(request.getProtocolVersion()).thenReturn("HTTP/1.1");
    when(request.getRemote()).thenReturn("10.0.0.1");
    when(request.getMethod()).thenReturn("POST");
    when(request.getRequestUri()).thenReturn("http://example.com/data");
    when(request.getHost()).thenReturn("example.com");
    when(request.getPath()).thenReturn("/data");
    when(request.getScheme()).thenReturn("http");
    when(request.getPort()).thenReturn(Optional.empty());
    when(request.getHeaders()).thenReturn(HttpHeaders.empty());
    when(request.getAttributes()).thenReturn(HttpAttributes.of("traceId", "t123"));
    when(request.getBodyAsString()).thenReturn("");
    when(request.getContentType()).thenReturn("text/plain");

    String result = formatter.format(precorrelation, request);

    assertNotNull(result);
  }

  @Test
  void format_request_nonJsonBody_returnsBodyAsString() throws IOException {
    when(precorrelation.getId()).thenReturn("corr-3");
    when(request.getOrigin()).thenReturn(Origin.LOCAL);
    when(request.getProtocolVersion()).thenReturn("HTTP/1.1");
    when(request.getRemote()).thenReturn("127.0.0.1");
    when(request.getMethod()).thenReturn("POST");
    when(request.getRequestUri()).thenReturn("http://example.com/upload");
    when(request.getHost()).thenReturn("example.com");
    when(request.getPath()).thenReturn("/upload");
    when(request.getScheme()).thenReturn("http");
    when(request.getPort()).thenReturn(Optional.empty());
    when(request.getHeaders()).thenReturn(HttpHeaders.empty());
    when(request.getAttributes()).thenReturn(HttpAttributes.EMPTY);
    when(request.getBodyAsString()).thenReturn("raw text body");
    when(request.getContentType()).thenReturn("text/plain");

    String result = formatter.format(precorrelation, request);

    assertNotNull(result);
  }

  @Test
  void format_request_sensitiveHeader_masked() throws IOException {
    when(precorrelation.getId()).thenReturn("corr-4");
    when(request.getOrigin()).thenReturn(Origin.LOCAL);
    when(request.getProtocolVersion()).thenReturn("HTTP/1.1");
    when(request.getRemote()).thenReturn("127.0.0.1");
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestUri()).thenReturn("http://example.com/secure");
    when(request.getHost()).thenReturn("example.com");
    when(request.getPath()).thenReturn("/secure");
    when(request.getScheme()).thenReturn("http");
    when(request.getPort()).thenReturn(Optional.empty());
    when(request.getHeaders()).thenReturn(HttpHeaders.of("authorization", "Bearer secret-token"));
    when(request.getAttributes()).thenReturn(HttpAttributes.EMPTY);
    when(request.getBodyAsString()).thenReturn("");
    when(request.getContentType()).thenReturn("application/json");
    when(logProperties.isSensitiveField("authorization")).thenReturn(true);

    String result = formatter.format(precorrelation, request);

    assertNotNull(result);
  }

  @Test
  void format_response_emptyHeadersAndBody_returnsJson() throws IOException {
    when(correlation.getId()).thenReturn("corr-5");
    when(correlation.getDuration()).thenReturn(java.time.Duration.ofMillis(100));
    when(response.getOrigin()).thenReturn(Origin.LOCAL);
    when(response.getProtocolVersion()).thenReturn("HTTP/1.1");
    when(response.getStatus()).thenReturn(200);
    when(response.getHeaders()).thenReturn(HttpHeaders.empty());
    when(response.getAttributes()).thenReturn(HttpAttributes.EMPTY);
    when(response.getBodyAsString()).thenReturn("");
    when(response.getContentType()).thenReturn("application/json");

    String result = formatter.format(correlation, response);

    assertNotNull(result);
  }

  @Test
  void format_request_jsonBody_masksAndParsesBody() throws IOException {
    when(precorrelation.getId()).thenReturn("corr-6");
    when(request.getOrigin()).thenReturn(Origin.LOCAL);
    when(request.getProtocolVersion()).thenReturn("HTTP/1.1");
    when(request.getRemote()).thenReturn("127.0.0.1");
    when(request.getMethod()).thenReturn("POST");
    when(request.getRequestUri()).thenReturn("http://example.com/login");
    when(request.getHost()).thenReturn("example.com");
    when(request.getPath()).thenReturn("/login");
    when(request.getScheme()).thenReturn("http");
    when(request.getPort()).thenReturn(Optional.empty());
    when(request.getHeaders()).thenReturn(HttpHeaders.empty());
    when(request.getAttributes()).thenReturn(HttpAttributes.EMPTY);
    when(request.getBodyAsString()).thenReturn("{\"password\":\"secret\"}");
    when(request.getContentType()).thenReturn("application/json");
    when(logProperties.getSensitiveFields()).thenReturn(Set.of("password"));
    when(gson.fromJson(any(String.class), any(Class.class))).thenReturn(Map.of("password", "****"));

    String result = formatter.format(precorrelation, request);

    assertNotNull(result);
  }
}
