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

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.nantaaditya.example.helper.DateTimeHelper;
import com.nantaaditya.example.helper.TsidHelper;
import com.nantaaditya.example.model.constant.HeaderConstant;
import com.nantaaditya.example.model.constant.ResponseCode;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.model.dto.JsonLogHttpRequest;
import com.nantaaditya.example.model.dto.JsonLogHttpResponse;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import tools.jackson.databind.ObjectMapper;

@Log4j2
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

  protected abstract String getClientId();

  @SneakyThrows
  protected ResultActions send(HttpMethod httpMethod, String path, Object request) {
    MockHttpServletRequestBuilder builder = buildRequest(httpMethod, path);

    if (builder == null) {
      throw new IllegalArgumentException("http httpMethod not valid");
    }

    String requestId = TsidHelper.generateTsid();
    String requestTime = DateTimeHelper.getDateInFormat(ZonedDateTime.now(), DateTimeHelper.ISO_8601_GMT7_FORMAT);

    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.put(HeaderConstant.CLIENT_ID.getHeader(), List.of(this.getClientId()));
    headers.put(HeaderConstant.REQUEST_ID.getHeader(), List.of(requestId));
    headers.put(HeaderConstant.REQUEST_TIME.getHeader(), List.of(requestTime));

    if (HTTP_METHODS_WITH_PAYLOAD.contains(httpMethod) && request != null) {
      builder
          .accept(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request));
    }

    JsonLogHttpRequest content = new JsonLogHttpRequest(
        httpMethod.name(),
        path,
        headers,
        HTTP_METHODS_WITH_PAYLOAD.contains(httpMethod) && request != null ? request : null
    );
    AppLogMessage appLogMessage = AppLogMessage.message("INCOMING").httpRequest(content);
    log(appLogMessage);

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

    Object body = null;
    try {
      body = objectMapper.readValue(response.getContentAsString(), Map.class);
    } catch (Exception e) {
      body = response.getContentAsString();
    }

    JsonLogHttpResponse content = new JsonLogHttpResponse(
        null,
        null,
        String.valueOf(response.getStatus()),
        null,
        appendHeader(response),
        body
    );
    log(AppLogMessage.message("OUTGOING").httpResponse(content));

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

  private MultiValueMap<String, String> appendHeader(MockHttpServletResponse response) {
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    for (String headerKey : response.getHeaderNames()) {
      headers.put(headerKey, List.of(response.getHeader(headerKey)));
    }
    return headers;
  }

  private void log(AppLogMessage appLogMessage) {
    log.info(appLogMessage);
  }
}

