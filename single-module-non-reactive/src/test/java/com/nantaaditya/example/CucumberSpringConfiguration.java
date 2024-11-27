package com.nantaaditya.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nantaaditya.example.helper.ApiHelper;
import com.nantaaditya.example.model.constant.HeaderConstant;
import com.nantaaditya.example.model.response.ContextResponse;
import com.nantaaditya.example.model.response.Response;
import io.cucumber.spring.CucumberContextConfiguration;
import java.io.UnsupportedEncodingException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;


@Slf4j
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@CucumberContextConfiguration
@AutoConfigureMockMvc
public class CucumberSpringConfiguration {

  @Autowired
  protected ApiHelper apiHelper;

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  protected ContextResponse context;

  protected HttpHeaders getRequestHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.put(HeaderConstant.CLIENT_ID.getHeader(), List.of(context.getHeaderRequest().getClientId()));
    headers.put(HeaderConstant.REQUEST_ID.getHeader(), List.of(context.getHeaderRequest().getRequestId()));
    headers.put(HeaderConstant.REQUEST_TIME.getHeader(), List.of(context.getHeaderRequest().getRequestTime()));
    return headers;
  }

  protected void assertResponseHeaders() {
    assertEquals(context.getHeaderRequest().getClientId(), context.getResponse().getHeader(HeaderConstant.CLIENT_ID.getHeader()));
    assertEquals(context.getHeaderRequest().getRequestId(), context.getResponse().getHeader(HeaderConstant.REQUEST_ID.getHeader()));
    assertNotNull(context.getResponse().getHeader(HeaderConstant.REQUEST_TIME.getHeader()));
    assertNotNull(context.getResponse().getHeader(HeaderConstant.RECEIVED_TIME.getHeader()));
    assertNotNull(context.getResponse().getHeader(HeaderConstant.RESPONSE_TIME.getHeader()));
  }

  protected <T> Response<T> getResponse(TypeReference<Response<T>> typeReference) {
    try {
      Response<T> response = objectMapper.readValue(
          this.context.getResponse().getContentAsString(),
          typeReference
      );
      return response;
    } catch (JsonProcessingException | UnsupportedEncodingException ex) {
      log.error("#TEST - failed convert response, ", ex);
      return null;
    }
  }
}
