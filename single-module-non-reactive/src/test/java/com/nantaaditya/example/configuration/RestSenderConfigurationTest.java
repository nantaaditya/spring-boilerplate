package com.nantaaditya.example.configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.nantaaditya.example.helper.RestClientHelper;
import com.nantaaditya.example.helper.RetryTemplateHelper;
import com.nantaaditya.example.properties.RetryProperties;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class RestSenderConfigurationTest {

  @InjectMocks
  private RestSenderConfiguration configuration;

  @Mock
  private RestClientHelper restClientHelper;

  @Mock
  private RetryTemplateHelper retryTemplateHelper;

  @Mock
  private RetryProperties retryProperties;

  private final ObjectMapper objectMapper = JsonMapper.builder().build();

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(configuration, "clientId", "test-app");
  }

  @Test
  void restSenderFactory_noClients_returnsNonNull() {
    when(restClientHelper.getClientNames()).thenReturn(Set.of());

    assertNotNull(configuration.restSenderFactory(
        restClientHelper, retryTemplateHelper, retryProperties, objectMapper));
  }
}
