package com.nantaaditya.example.helper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.model.dto.JsonLogHttpRequest;
import com.nantaaditya.example.model.dto.JsonLogHttpResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpLogWriter;
import org.zalando.logbook.Precorrelation;

@Log4j2
public class ApiLogbookWriter implements HttpLogWriter {

  private final ObjectMapper mapper;

  public ApiLogbookWriter(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void write(Precorrelation precorrelation, String request) throws IOException {
    try {
      Map<String, Object> content = mapper.readValue(request, LinkedHashMap.class);
      String method = (String) content.get("method");
      String url = (String) content.get("uri");
      MultiValueMap<String, String> headers = getHeaders(content);
      Object body = content.get("body");
      JsonLogHttpRequest jsonLogHttpRequest = new JsonLogHttpRequest(
          method,
          url,
          headers,
          body
      );

      log.info(AppLogMessage.create("#Zalando: incoming request", jsonLogHttpRequest, content));
    } catch (Exception e) {
      log.info(AppLogMessage.create(request, e));
    }
  }

  @Override
  public void write(Correlation correlation, String response) throws IOException {
    try {
      Map<String, Object> content = mapper.readValue(response, LinkedHashMap.class);
      int status = (int) content.get("status");
      int duration =  (int) content.get("duration");
      MultiValueMap<String, String> headers = getHeaders(content);
      Object body = content.get("body");
      JsonLogHttpResponse jsonLogHttpResponse = new JsonLogHttpResponse(
          null,
          null,
          String.valueOf(status),
          String.valueOf(duration).concat(" ms"),
          headers,
          body
      );

      log.info(AppLogMessage.create("#Zalando: outgoing response", jsonLogHttpResponse, content));
    } catch (Exception e) {
      log.info(AppLogMessage.create(response, e));
    }
  }

  private MultiValueMap<String, String> getHeaders(Map<String, Object> content) {
    Map<String, List<String>> headers = mapper.convertValue(content.get("headers"), new TypeReference<Map<String, List<String>>>() {});
    MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
    for (String key : headers.keySet()) {
      multiValueMap.put(key, headers.get(key));
    }
    return multiValueMap;
  }
}
