package com.nantaaditya.example.steps.common;

import com.nantaaditya.example.CucumberSpringConfiguration;
import io.cucumber.java.en.And;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestStep extends CucumberSpringConfiguration {

  @And("with {} name payload")
  public void withNamePayload(String name) {
    log.debug("#TEST - set name payload {}", name);
    this.context.getExampleRequest().setName(name);
  }

  @And("with {} age payload")
  public void withAgePayload(int age) {
    log.debug("#TEST - set age payload {}", age);
    this.context.getExampleRequest().setAge(age);
  }
}
