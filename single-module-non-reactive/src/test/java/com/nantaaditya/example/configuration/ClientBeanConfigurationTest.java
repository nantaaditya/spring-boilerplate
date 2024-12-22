package com.nantaaditya.example.configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
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
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class ClientBeanConfigurationTest {

  @InjectMocks
  private ClientBeanConfiguration clientBeanConfiguration;
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

  @BeforeEach
  void setUp() {
    clientConfiguration = new ClientConfiguration(
        "http://google.com",
        new ClientTimeOutConfiguration(1000, 2000, 3000),
        new ClientProxyConfiguration("http://localhost", 9090),
        new ClientCredentialConfiguration("username", "password"),
        true
    );
    lenient().when(clientProperties.pooling())
        .thenReturn(new ClientPoolingConfiguration(25, 5));
  }

  @Test
  void onStart() {
    clientConfiguration = new ClientConfiguration(
        "http://google.com",
        new ClientTimeOutConfiguration(1000, 2000, 3000),
        new ClientProxyConfiguration(null, 0),
        new ClientCredentialConfiguration("username", "password"),
        false
    );

    when(clientProperties.configurations())
        .thenReturn(Map.of("default", clientConfiguration));
    doNothing().when(applicationContext)
        .registerBean(anyString(), eq(RestClient.class), any(Supplier.class), any(BeanDefinitionCustomizer.class));
    when(clientProperties.getBeanNames(anyString()))
        .thenReturn(Set.of("default"));

    clientBeanConfiguration.onStart();

    verify(clientProperties, atLeast(1)).configurations();
    verify(clientProperties).getBeanNames(anyString());
  }

  @Test
  void createRestClient() {
    assertNotNull(clientBeanConfiguration.createRestClient(clientConfiguration));
  }
}
