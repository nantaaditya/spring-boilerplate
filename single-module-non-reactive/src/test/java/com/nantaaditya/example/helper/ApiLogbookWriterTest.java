package com.nantaaditya.example.helper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.Precorrelation;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ApiLogbookWriterTest {

  private ApiLogbookWriter writer;

  @Mock
  private ObjectMapper mapper;

  @Mock
  private Precorrelation precorrelation;

  @Mock
  private Correlation correlation;

  @BeforeEach
  void setUp() {
    writer = new ApiLogbookWriter(mapper);
  }

  @Test
  void write_request_validJson_parsesAndLogs() throws IOException {
    LinkedHashMap<String, Object> parsed = new LinkedHashMap<>();
    parsed.put("method", "GET");
    parsed.put("uri", "/test");
    parsed.put("headers", Map.of("content-type", List.of("application/json")));
    parsed.put("body", null);
    Map<String, List<String>> headerMap = Map.of("content-type", List.of("application/json"));

    when(mapper.readValue(eq("{\"method\":\"GET\"}"), eq(LinkedHashMap.class))).thenReturn(parsed);
    when(mapper.convertValue(any(), any(TypeReference.class))).thenReturn(headerMap);

    writer.write(precorrelation, "{\"method\":\"GET\"}");

    verify(mapper).readValue(eq("{\"method\":\"GET\"}"), eq(LinkedHashMap.class));
  }

  @Test
  void write_request_malformedJson_doesNotThrow() throws IOException {
    when(mapper.readValue(eq("not-json"), eq(LinkedHashMap.class)))
        .thenThrow(new RuntimeException("parse error"));

    writer.write(precorrelation, "not-json");

    verify(mapper).readValue(eq("not-json"), eq(LinkedHashMap.class));
  }

  @Test
  void write_response_validJson_parsesAndLogs() throws IOException {
    LinkedHashMap<String, Object> parsed = new LinkedHashMap<>();
    parsed.put("status", 200);
    parsed.put("duration", 50);
    parsed.put("headers", Map.of("content-type", List.of("application/json")));
    parsed.put("body", null);
    Map<String, List<String>> headerMap = Map.of("content-type", List.of("application/json"));

    when(mapper.readValue(eq("{\"status\":200}"), eq(LinkedHashMap.class))).thenReturn(parsed);
    when(mapper.convertValue(any(), any(TypeReference.class))).thenReturn(headerMap);

    writer.write(correlation, "{\"status\":200}");

    verify(mapper).readValue(eq("{\"status\":200}"), eq(LinkedHashMap.class));
  }

  @Test
  void write_response_malformedJson_doesNotThrow() throws IOException {
    when(mapper.readValue(eq("bad"), eq(LinkedHashMap.class)))
        .thenThrow(new RuntimeException("parse error"));

    writer.write(correlation, "bad");

    verify(mapper).readValue(eq("bad"), eq(LinkedHashMap.class));
  }
}
