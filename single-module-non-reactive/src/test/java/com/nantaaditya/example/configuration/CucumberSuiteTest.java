package com.nantaaditya.example.configuration;

import io.cucumber.core.options.Constants;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Slf4j
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(
    key = Constants.PLUGIN_PROPERTY_NAME,
    value = "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:")
@ConfigurationParameter(
    key = Constants.GLUE_PROPERTY_NAME,
    value = "com.nantaaditya.example"
)
public class CucumberSuiteTest {

  @Before
  public void beforeTest(Scenario scenario) {
    log.info("#TEST - scenario [{}] is start", scenario.getName());
  }

  @After
  public void afterTest(Scenario scenario) {
    log.info("#TEST - scenario [{}] is [{}]", scenario.getName(), scenario.getStatus());
  }
}
