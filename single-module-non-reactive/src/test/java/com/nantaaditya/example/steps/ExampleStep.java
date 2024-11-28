package com.nantaaditya.example.steps;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nantaaditya.example.CucumberSpringConfiguration;
import com.nantaaditya.example.model.response.ExampleResponse;
import com.nantaaditya.example.model.response.Response;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;

@Slf4j
public class ExampleStep extends CucumberSpringConfiguration {

  @Given("a request GET example")
  public void composeGetEndpoint() {
    log.info("#TEST - request GET /example endpoint");
  }

  @Given("a request POST example")
  public void composePostEndpoint() {
    log.info("#TEST - request POST /example endpoint");
  }

  @When("I hit GET example endpoint")
  public void hitGetEndpoint() {
    this.context.setResponse(apiHelper.call(
        HttpMethod.GET,
        "/api/example",
        getRequestHeaders(),
        null
      )
    );
  }

  @When("I hit POST example endpoint")
  public void hitPostEndpoint() {
    this.context.setResponse(apiHelper.call(
        HttpMethod.POST,
        "/api/example",
        getRequestHeaders(),
        this.context.getExampleRequest()
      )
    );
  }

  @And("I receive a GET response")
  public void assertGetResponse() {
    assertNotNull(this.context);
    assertNotNull(getResponse(new TypeReference<Response<String>>() {}));
  }

  @Then("I receive a POST response")
  public void assertPostResponse() {
    assertNotNull(context);
    assertNotNull(getResponse(new TypeReference<Response<ExampleResponse>>() {}));
  }

}
