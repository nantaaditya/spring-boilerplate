package com.nantaaditya.example.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
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
      log.info("#TEST - execute call");
      MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.request(httpMethod, path);

      if (Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH).contains(httpMethod) && body != null) {
        builder
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body));
      }

      MockHttpServletResponse response = mockMvc.perform(builder
              .accept(MediaType.APPLICATION_JSON)
              .headers(headers)
          )
          .andReturn()
          .getResponse();

      log.info("#TEST - http code {}", response.getStatus());
      log.info("#TEST - response {}", response.getContentAsString());
      return response;
    } catch (Exception e) {
      log.error("#TEST - execute call error, ", e);
      return null;
    }
  }
}
