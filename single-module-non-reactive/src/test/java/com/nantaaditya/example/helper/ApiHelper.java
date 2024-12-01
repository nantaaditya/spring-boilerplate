package com.nantaaditya.example.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@Slf4j
@Component
public class ApiHelper {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  public MockHttpServletResponse call(HttpMethod httpMethod, String path, HttpHeaders headers, Object body) {
    try {
      StringBuilder requestLog = new StringBuilder("\n");
      requestLog.append("=".repeat(50));
      requestLog.append("\n");
      requestLog.append("#API - request");
      requestLog.append("\n");
      requestLog.append(String.format("[%s] - %s", httpMethod.name(), path));
      requestLog.append("\n");

      MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.request(httpMethod, path);

      if (Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH).contains(httpMethod) && body != null) {
        requestLog.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(body));
        requestLog.append("\n");
        builder
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body));
      }

      requestLog.append("\n");
      requestLog.append("#API - response");
      requestLog.append("\n");
      requestLog.append("=".repeat(50));
      requestLog.append("\n");

      MockHttpServletResponse response = mockMvc.perform(builder
              .accept(MediaType.APPLICATION_JSON)
              .headers(headers)
          )
          .andReturn()
          .getResponse();

      requestLog.append(String.format("status [%s]", response.getStatus()));
      requestLog.append("\n");
      Map<String, Object> maps = objectMapper.readValue(response.getContentAsString(), Map.class);
      requestLog.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(maps));
      requestLog.append("\n");
      requestLog.append("=".repeat(50));
      requestLog.append("\n");
      log.info(requestLog.toString());

      return response;
    } catch (Exception e) {
      log.error("#TEST - execute call error, ", e);
      return null;
    }
  }
}
