package com.nantaaditya.example.steps.common;

import com.nantaaditya.example.CucumberSpringConfiguration;
import io.cucumber.java.en.And;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HeaderStep extends CucumberSpringConfiguration {

  @And("with {} clientId request header")
  public void withClientIdHeader(String clientId) {
    log.debug("#TEST - set client id header {}", clientId);
    this.context.getHeaderRequest().setClientId(clientId);
  }

  @And("with {} requestId request header")
  public void withRequestIdHeader(String requestId) {
    log.debug("#TEST - set request id header {}", requestId);
    this.context.getHeaderRequest().setRequestId(requestId);
  }

  @And("with {} requestTime request header")
  public void withRequestTimeHeader(String requestTime) {
    log.debug("#TEST - set request time header {}", requestTime);
    this.context.getHeaderRequest().setRequestTime(requestTime);
  }

}
