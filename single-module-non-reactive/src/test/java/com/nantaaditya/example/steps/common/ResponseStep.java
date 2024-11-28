package com.nantaaditya.example.steps.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nantaaditya.example.CucumberSpringConfiguration;
import com.nantaaditya.example.model.response.ExampleResponse;
import com.nantaaditya.example.model.response.Response;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResponseStep extends CucumberSpringConfiguration {

  @Then("I receive a {} http status response")
  public void andAssertHttpStatusResponse(int code) {
    assertEquals(code, context.getResponse().getStatus());
  }

  @And("I receive a response headers")
  public void andAssertResponseHeader() {
    assertResponseHeaders();
  }

  @And("I receive a {} response code")
  public void andAssertResponseCode(String responseCode) {
    Response<ExampleResponse> response = getResponse(
        new TypeReference<Response<ExampleResponse>>() {}
    );
    assertNotNull(response);
    assertEquals(responseCode, response.getResponse().getCode());
  }

  @And("I receive a {} response body")
  public void andAssertResponseData(String name) {
    Response<ExampleResponse> response = getResponse(
        new TypeReference<Response<ExampleResponse>>() {}
    );
    assertNotNull(response);
    if (!name.isBlank()) {
      assertEquals(name, response.getData().name());
    }
  }

  @And("I receive a {} and {} error response")
  public void andReceiveErrorResponse(String violationKey, String violationError) {
    Response<ExampleResponse> response = getResponse(
        new TypeReference<Response<ExampleResponse>>() {}
    );
    assertNotNull(response);
    if (!violationKey.isBlank()) {
      assertEquals(violationError, response.getError().getViolations().get(violationKey).get(0));
    }
  }
}
