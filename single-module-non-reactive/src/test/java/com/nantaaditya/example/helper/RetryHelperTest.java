package com.nantaaditya.example.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nantaaditya.example.api.BaseIntegrationTest;
import com.nantaaditya.example.configuration.RetryTemplateConfiguration;
import com.nantaaditya.example.model.constant.BackoffPolicyConstant;
import com.nantaaditya.example.properties.embedded.RetryConfiguration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.retry.RetryTemplate;

@ExtendWith(MockitoExtension.class)
@Order(1)
class RetryHelperTest extends BaseIntegrationTest {

  @Autowired
  private RetryTemplateConfiguration retryTemplateConfiguration;

  @Autowired
  private RetryTemplateHelper retryTemplateHelper;

  @Autowired
  private RetryHelper retryHelper;

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
        100,
        1.0,
        5000,
        3,
        "java.lang.IllegalArgumentException:true"
    );

    retryTemplate = retryTemplateConfiguration.createRetryTemplate("default", retryConfiguration);
  }

  @Test
  @DisplayName("execute with valid request returns result without triggering retry")
  void execute_withoutRetry() {
    assertThat(retryHelper.execute(retryTemplate, retryConfiguration.maxAttempt(), "type", "name", action, fallback, "Hello World"))
        .isNotNull();
  }

  @Test
  @DisplayName("execute with exponential backoff retries maxAttempt times then throws original exception")
  void execute_retryExponential() {
    AtomicInteger counter = new AtomicInteger(0);
    Function<String, String> retryAction = (String request) -> {
      counter.incrementAndGet();
      throw new IllegalArgumentException("error");
    };

    retryConfiguration = new RetryConfiguration(
        BackoffPolicyConstant.EXPONENTIAL,
        100,
        1.0,
        5000,
        3,
        "java.lang.IllegalArgumentException:true"
    );

    retryTemplate = retryTemplateConfiguration.createRetryTemplate("default", retryConfiguration);
    assertThatThrownBy(() -> retryHelper.execute(retryTemplate, retryConfiguration.maxAttempt(), "type", "name", retryAction, null, "Error"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(counter.get()).isEqualTo(4);
  }

  @Test
  @DisplayName("execute with fixed backoff retries maxAttempt times then throws original exception")
  void execute_retryFixed() {
    AtomicInteger counter = new AtomicInteger(0);
    Function<String, String> retryAction = (String request) -> {
      counter.incrementAndGet();
      throw new IllegalArgumentException("error");
    };

    retryConfiguration = new RetryConfiguration(
        BackoffPolicyConstant.FIXED,
        100,
        1.0,
        5000,
        3,
        "java.lang.IllegalArgumentException:true"
    );

    retryTemplate = retryTemplateConfiguration.createRetryTemplate("default", retryConfiguration);
    assertThatThrownBy(() -> retryHelper.execute(retryTemplate, retryConfiguration.maxAttempt(), "type", "name", retryAction, null, "Error"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(counter.get()).isEqualTo(4);
  }

  @Test
  @DisplayName("execute with uniform random backoff retries maxAttempt times then throws original exception")
  void execute_retryUniformRandom() {
    AtomicInteger counter = new AtomicInteger(0);
    Function<String, String> retryAction = (String request) -> {
      counter.incrementAndGet();
      throw new IllegalArgumentException("error");
    };

    retryConfiguration = new RetryConfiguration(
        BackoffPolicyConstant.UNIFORM_RANDOM,
        100,
        1.0,
        5000,
        3,
        "java.lang.IllegalArgumentException:true"
    );

    retryTemplate = retryTemplateConfiguration.createRetryTemplate("default", retryConfiguration);
    assertThatThrownBy(() -> retryHelper.execute(retryTemplate, retryConfiguration.maxAttempt(), "type", "name", retryAction, null, "Error"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(counter.get()).isEqualTo(4);
  }

  @Test
  @DisplayName("execute with exponential random backoff retries maxAttempt times then throws original exception")
  void execute_retryExponentialRandom() {
    AtomicInteger counter = new AtomicInteger(0);
    Function<String, String> retryAction = (String request) -> {
      counter.incrementAndGet();
      throw new IllegalArgumentException("error");
    };

    retryConfiguration = new RetryConfiguration(
        BackoffPolicyConstant.EXPONENTIAL_RANDOM,
        100,
        1.0,
        5000,
        3,
        "java.lang.IllegalArgumentException:true"
    );

    retryTemplate = retryTemplateConfiguration.createRetryTemplate("default", retryConfiguration);
    assertThatThrownBy(() -> retryHelper.execute(retryTemplate, retryConfiguration.maxAttempt(), "type", "name", retryAction, null, "Error"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(counter.get()).isEqualTo(4);
  }

  @Test
  @DisplayName("execute with fallback invokes fallback on first failure without retry")
  void execute_withFallback() {
    AtomicInteger counter = new AtomicInteger(0);
    Function<String, String> retryAction = (String request) -> {
      counter.incrementAndGet();
      throw new IllegalArgumentException("error");
    };

    retryConfiguration = new RetryConfiguration(
        BackoffPolicyConstant.FIXED,
        100,
        1.0,
        5000,
        3,
        "java.lang.IllegalArgumentException:true"
    );

    retryTemplate = retryTemplateConfiguration.createRetryTemplate("default", retryConfiguration);
    String result = retryHelper.execute(retryTemplate, retryConfiguration.maxAttempt(), "type", "name", retryAction, fallback, "Error");
    assertThat(result).isEqualTo("error");
    assertThat(counter.get()).isEqualTo(1);
  }

  @Test
  @DisplayName("execute with excluded exception throws immediately without retry")
  void execute_excludeException() {
    AtomicInteger counter = new AtomicInteger(0);
    Function<String, String> retryAction = (String request) -> {
      counter.incrementAndGet();
      throw new RuntimeException("runtime error");
    };

    retryConfiguration = new RetryConfiguration(
        BackoffPolicyConstant.FIXED,
        100,
        1.0,
        5000,
        3,
        "java.lang.RuntimeException:false"
    );

    retryTemplate = retryTemplateConfiguration.createRetryTemplate("default", retryConfiguration);
    assertThatThrownBy(() -> retryHelper.execute(retryTemplate, retryConfiguration.maxAttempt(), "type", "name", retryAction, null, "Error"))
        .isInstanceOf(RuntimeException.class);
    assertThat(counter.get()).isEqualTo(1);
  }

  @Test
  @DisplayName("retryTemplateHelper provides configured RetryTemplate bean")
  void assertBeanCreation() {
    assertThat(retryTemplateHelper.getRetryTemplate("default")).isNotNull();
  }
}