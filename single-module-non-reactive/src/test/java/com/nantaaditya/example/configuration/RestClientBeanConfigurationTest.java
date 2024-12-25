package com.nantaaditya.example.configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.nantaaditya.example.properties.ClientProperties;
import com.nantaaditya.example.properties.LogProperties;
import com.nantaaditya.example.properties.embedded.ClientConfiguration;
import com.nantaaditya.example.properties.embedded.ClientCredentialConfiguration;
import com.nantaaditya.example.properties.embedded.ClientPoolingConfiguration;
import com.nantaaditya.example.properties.embedded.ClientProxyConfiguration;
import com.nantaaditya.example.properties.embedded.ClientTimeOutConfiguration;
import io.micrometer.observation.ObservationRegistry;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

  private ClientConfiguration clientConfiguration;

  @Test
  void createRestClient() {
    clientConfiguration = new ClientConfiguration(
        "http://google.com",
        new ClientTimeOutConfiguration(1000, 2000, 3000),
        new ClientProxyConfiguration("http://localhost", 9090),
        new ClientCredentialConfiguration("username", "password"),
        true
    );
    lenient().when(clientProperties.pooling())
        .thenReturn(new ClientPoolingConfiguration(25, 5));
    assertNotNull(restClientBeanConfiguration.createRestClient(clientConfiguration));
  }

  @Test
  void restClientHelperFactory_null() {
    when(clientProperties.configurations())
        .thenReturn(null);
    assertNotNull(restClientBeanConfiguration.restClientHelperFactory());
    verify(clientProperties).configurations();
  }

  @Test
  void restClientHelperFactory_empty() {
    when(clientProperties.configurations())
        .thenReturn(Collections.emptyMap());
    assertNotNull(restClientBeanConfiguration.restClientHelperFactory());
    verify(clientProperties, atLeast(1)).configurations();
  }
}
