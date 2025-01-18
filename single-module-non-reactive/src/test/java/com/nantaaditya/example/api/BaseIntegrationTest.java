package com.nantaaditya.example.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.nantaaditya.example.helper.DateTimeHelper;
import com.nantaaditya.example.helper.TsidHelper;
import com.nantaaditya.example.model.constant.HeaderConstant;
import com.nantaaditya.example.model.constant.ResponseCode;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@Slf4j
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ExtendWith(SpringExtension.class)
public abstract class BaseIntegrationTest {

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private Flyway flyway;

  private static final Set<HttpMethod> HTTP_METHODS_WITH_PAYLOAD = Set.of(POST, PUT, PATCH);
  private static final String BREAKPOINT = "\n";

  protected abstract String getClientId();

  @SneakyThrows
  protected ResultActions send(HttpMethod httpMethod, String path, Object request) {
    StringBuilder logContent = new StringBuilder(String.format("#Request - [%s] %s", httpMethod, path));
    MockHttpServletRequestBuilder builder = buildRequest(httpMethod, path);

    if (builder == null) {
      throw new IllegalArgumentException("http method not valid");
    }

    String requestId = TsidHelper.generateTsid();
    String requestTime = DateTimeHelper.getDateInFormat(ZonedDateTime.now(), DateTimeHelper.ISO_8601_GMT7_FORMAT);

    logContent
        .append(BREAKPOINT)
        .append(HeaderConstant.CLIENT_ID.getHeader()).append(": ").append(getClientId())
        .append(BREAKPOINT)
        .append(HeaderConstant.REQUEST_ID.getHeader()).append(": ").append(requestId)
        .append(BREAKPOINT)
        .append(HeaderConstant.REQUEST_TIME.getHeader()).append(": ").append(requestTime);

    if (HTTP_METHODS_WITH_PAYLOAD.contains(httpMethod) && request != null) {
      logContent.append(BREAKPOINT)
          .append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));
      builder
          .accept(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request));
    }

    log.info(logContent.toString());

    return mockMvc.perform(
        builder
            .contentType(MediaType.APPLICATION_JSON)
            .header(HeaderConstant.CLIENT_ID.getHeader(), getClientId())
            .header(HeaderConstant.REQUEST_ID.getHeader(), requestId)
            .header(HeaderConstant.REQUEST_TIME.getHeader(), requestTime)
    );
  }

  @SneakyThrows
  protected void mock(HttpMethod httpMethod, String path, HttpStatus httpStatus, Object response, int delay) {
    ResponseDefinitionBuilder responseDefinitionBuilder = WireMock.aResponse()
        .withStatus(httpStatus.value())
        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .withHeader(HttpHeaders.CONNECTION, "close")
        .withBody(objectMapper.writeValueAsString(response));

    if (delay>0) {
      responseDefinitionBuilder = responseDefinitionBuilder.withFixedDelay(delay);
    }

    WireMock.stubFor(
        WireMock.request(
                httpMethod.toString(),
                WireMock.urlEqualTo(path)
            )
            .willReturn(
                responseDefinitionBuilder
            )
    );
  }

  @SneakyThrows
  protected void assertResult(ResultActions resultActions, HttpStatus httpStatus,
      ResponseCode responseCode, ResultMatcher... dataResultMatcher) {

    MockHttpServletResponse response = resultActions.andReturn().getResponse();
    StringBuilder logConstant = new StringBuilder(String.format("#Response - [%s]", response.getStatus()));
    appendHeader(response, logConstant);
    appendBody(response, logConstant);
    log.info(logConstant.toString());

    resultActions
        .andExpect(status().is(httpStatus.value()))
        .andExpect(header().exists(HeaderConstant.RECEIVED_TIME.getHeader()))
        .andExpect(header().exists(HeaderConstant.RESPONSE_TIME.getHeader()))
        .andExpect(jsonPath("$.response.code", equalTo(responseCode.getCode())));

    if (dataResultMatcher != null) {
      resultActions.andExpectAll(dataResultMatcher);
    }
  }

  private MockHttpServletRequestBuilder buildRequest(HttpMethod httpMethod, String path) {
    if (POST == httpMethod) {
      return MockMvcRequestBuilders.post(path);
    }

    if (PUT == httpMethod) {
      return MockMvcRequestBuilders.put(path);
    }

    if (PATCH == httpMethod) {
      return MockMvcRequestBuilders.patch(path);
    }

    if (GET == httpMethod) {
      return MockMvcRequestBuilders.get(path);
    }

    if (DELETE == httpMethod) {
      return MockMvcRequestBuilders.delete(path);
    }

    return null;
  }

  private void appendHeader(MockHttpServletResponse response, StringBuilder logConstant) {
    for (String headerKey : response.getHeaderNames()) {
      logConstant.append(BREAKPOINT)
          .append(headerKey).append(": ").append(response.getHeaders(headerKey));
    }
  }

  @SneakyThrows
  private void appendBody(MockHttpServletResponse response, StringBuilder logConstant) {
    Map<String, Object> content = objectMapper.readValue(response.getContentAsString(), Map.class);
    logConstant.append(BREAKPOINT)
        .append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(content));
  }
}

