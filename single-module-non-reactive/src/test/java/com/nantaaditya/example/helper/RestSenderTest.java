package com.nantaaditya.example.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.nantaaditya.example.model.observation.ObservableRetryable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.retry.Retryable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class RestSenderTest {

  @Mock
  private RestClient restClient;

  private final ObjectMapper objectMapper = JsonMapper.builder().build();
  private final ParameterizedTypeReference<String> stringType = new ParameterizedTypeReference<>() {
  };

  private RestSender senderWithRetry(RetryTemplate retryTemplate) {
    return new RestSender.Builder("client-id", "test-client", restClient, objectMapper)
        .retryTemplate(retryTemplate)
        .build();
  }

  private RestSender senderWithoutRetry() {
    return new RestSender.Builder("client-id", "test-client", restClient, objectMapper).build();
  }

  @Test
  @DisplayName("executeWithRetry without retryTemplate throws IllegalArgumentException")
  void executeWithRetry_noRetryTemplate_throwsIllegalArgument() {
    RestSender sender = senderWithoutRetry();

    assertThatThrownBy(() ->
        sender.executeWithRetry(HttpMethod.GET, "/test", new HttpHeaders(), null, stringType,
            "process"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("retryTemplate not set");
  }

  @Test
  @DisplayName("executeWithRetry routes through ObservableRetryable so listener can observe")
  @SuppressWarnings("unchecked")
  void executeWithRetry_wrapsCallInObservableRetryable() throws Exception {
    RetryTemplate retryTemplate = spy(new RetryTemplate());
    RuntimeException rootCause = new RuntimeException("downstream failure");
    doThrow(new RetryException("exhausted", rootCause))
        .when(retryTemplate).execute(any(Retryable.class));

    RestSender sender = senderWithRetry(retryTemplate);

    assertThatThrownBy(() ->
        sender.executeWithRetry(HttpMethod.GET, "/test", new HttpHeaders(), null, stringType,
            "process"))
        .isSameAs(rootCause);

    ArgumentCaptor<Retryable<?>> captor = ArgumentCaptor.forClass(Retryable.class);
    verify(retryTemplate).execute(captor.capture());
    assertThat(captor.getValue()).isInstanceOf(ObservableRetryable.class);
  }

  @Test
  @DisplayName("executeWithRetry unwraps RetryException to its cause")
  @SuppressWarnings("unchecked")
  void executeWithRetry_unwrapsRetryException() throws Exception {
    RetryTemplate retryTemplate = spy(new RetryTemplate());
    RuntimeException cause = new IllegalStateException("real cause");
    doThrow(new RetryException("wrap", cause))
        .when(retryTemplate).execute(any(Retryable.class));

    RestSender sender = senderWithRetry(retryTemplate);

    assertThatThrownBy(() ->
        sender.executeWithRetry(HttpMethod.POST, "/api", new HttpHeaders(), "body", stringType,
            "op"))
        .isSameAs(cause);
  }
}
