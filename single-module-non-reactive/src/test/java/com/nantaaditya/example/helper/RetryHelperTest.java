package com.nantaaditya.example.helper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nantaaditya.example.api.BaseIntegrationTest;
import com.nantaaditya.example.configuration.RetryTemplateConfiguration;
import com.nantaaditya.example.model.constant.BackoffPolicyConstant;
import com.nantaaditya.example.properties.embedded.RetryConfiguration;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;

@ExtendWith(MockitoExtension.class)
@Order(1)
class RetryHelperTest extends BaseIntegrationTest {

  @Autowired
  private RetryTemplateConfiguration retryTemplateConfiguration;

  @Autowired
  private RetryTemplateHelper retryTemplateHelper;

  private RetryConfiguration retryConfiguration;
  private RetryTemplate retryTemplate;

  private final Function<String, String> action = (String request) -> {
    if (request.equals("Hello World")) return request;
    else throw new IllegalArgumentException("error");
  };
  private final Function<IllegalArgumentException, String> fallback = Throwable::getMessage;

  @Override
  protected String getClientId() {
    return "retry-helper";
  }

  @BeforeEach
  void setUp() {
    retryConfiguration = new RetryConfiguration(
        BackoffPolicyConstant.EXPONENTIAL,
        1000,
        0.2,
        10000,
        3,
        "java.lang.IllegalArgumentException:true"
    );

    retryTemplate = retryTemplateConfiguration.createRetryTemplate("default", retryConfiguration);
  }

  @Test
  void execute_withoutRetry() {
    assertNotNull(RetryHelper.execute(retryTemplate, "type", "name", action, fallback, "Hello World"));
  }

  @Test
  void execute_retryExponential() {
    retryConfiguration = new RetryConfiguration(
        BackoffPolicyConstant.EXPONENTIAL,
        1000,
        0.2,
        1000,
        3,
        "java.lang.IllegalArgumentException:true"
    );

    retryTemplate = retryTemplateConfiguration.createRetryTemplate("default", retryConfiguration);
    assertThrows(IllegalArgumentException.class, () -> RetryHelper.execute(retryTemplate, "type", "name", action, fallback, "Error"));
  }

  @Test
  void execute_retryFixed() {
    retryConfiguration = new RetryConfiguration(
        BackoffPolicyConstant.FIXED,
        100,
        0.2,
        1000,
        3,
        "java.lang.IllegalArgumentException:true"
    );

    retryTemplate = retryTemplateConfiguration.createRetryTemplate("default", retryConfiguration);
    assertThrows(IllegalArgumentException.class, () -> RetryHelper.execute(retryTemplate, "type", "name", action, fallback,"Error"));
  }

  @Test
  void execute_retryUniformRandom() {
    retryConfiguration = new RetryConfiguration(
        BackoffPolicyConstant.UNIFORM_RANDOM,
        100,
        0.2,
        1000,
        3,
        "java.lang.IllegalArgumentException:true"
    );

    retryTemplate = retryTemplateConfiguration.createRetryTemplate("default", retryConfiguration);
    assertThrows(IllegalArgumentException.class, () -> RetryHelper.execute(retryTemplate, "type", "name", action, fallback, "Error"));
  }

  @Test
  void execute_retryExponentialRandom() {
    retryConfiguration = new RetryConfiguration(
        BackoffPolicyConstant.EXPONENTIAL_RANDOM,
        100,
        0.2,
        1000,
        3,
        "java.lang.IllegalArgumentException:true"
    );

    retryTemplate = retryTemplateConfiguration.createRetryTemplate("default", retryConfiguration);
    assertThrows(IllegalArgumentException.class, () -> RetryHelper.execute(retryTemplate, "type", "name", action, fallback,"Error"));
  }

  @Test
  void assertBeanCreation() {
    assertNotNull(retryTemplateHelper.getRetryTemplate("default"));
  }
}