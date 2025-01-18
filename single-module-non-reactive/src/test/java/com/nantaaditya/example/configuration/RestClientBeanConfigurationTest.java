package com.nantaaditya.example.configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.nantaaditya.example.properties.ClientProperties;
import com.nantaaditya.example.properties.LogProperties;
import com.nantaaditya.example.properties.embedded.ClientConfiguration;
import io.micrometer.observation.ObservationRegistry;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.support.GenericApplicationContext;

@ExtendWith(MockitoExtension.class)
class RestClientBeanConfigurationTest {

  @InjectMocks
  private RestClientBeanConfiguration restClientBeanConfiguration;
  @Mock
  private Gson gson;
  @Mock
  private ClientProperties clientProperties;
  @Mock
  private LogProperties logProperties;
  @Mock
  private ObservationRegistry observationRegistry;
  @Mock
  private GenericApplicationContext applicationContext;
  @Mock
  private RestTemplateBuilder builder;

  private ClientConfiguration clientConfiguration;

  @Test
  void restClientHelperFactory_null() {
    when(clientProperties.configurations())
        .thenReturn(null);
    assertNotNull(restClientBeanConfiguration.restClientHelperFactory(builder));
    verify(clientProperties).configurations();
  }

  @Test
  void restClientHelperFactory_empty() {
    when(clientProperties.configurations())
        .thenReturn(Collections.emptyMap());
    assertNotNull(restClientBeanConfiguration.restClientHelperFactory(builder));
    verify(clientProperties, atLeast(1)).configurations();
  }
}
