package com.nantaaditya.example.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.nantaaditya.example.model.constant.ResponseCode;
import com.nantaaditya.example.model.request.ExampleRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultActions;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(0)
public class ExampleApiTest extends BaseIntegrationTest {

  @Override
  protected String getClientId() {
    return "clientId";
  }

  @Test
  @Order(0)
  void getExample() {
    ResultActions result = send(HttpMethod.GET, "/api/example", null);
    assertResult(result, HttpStatus.OK, ResponseCode.SUCCESS, jsonPath("$.data", equalTo("Hello world")));
  }

  @Test
  @Order(1)
  void postExample_invalidParams() {
    ExampleRequest request = new ExampleRequest(null, -1);
    ResultActions result = send(HttpMethod.POST, "/api/example", request);
    assertResult(result, HttpStatus.BAD_REQUEST, ResponseCode.INVALID_PARAMS, jsonPath("$.error.violations.name[0]", equalTo("NotBlank")));
  }

  @Test
  @Order(2)
  void postExample_success() {
    ExampleRequest request = new ExampleRequest("name", 1);
    ResultActions result = send(HttpMethod.POST, "/api/example", request);
    assertResult(result, HttpStatus.OK, ResponseCode.SUCCESS, jsonPath("$.data.name", equalTo("name")));
  }

  @Test
  @Order(3)
  void getMock_success() {
    ResultActions result = send(HttpMethod.GET, "/api/example/mock", null);
    assertResult(result, HttpStatus.OK, ResponseCode.SUCCESS, jsonPath("$.data.completed", equalTo(false)));
  }
}
