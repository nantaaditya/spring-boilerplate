package com.nantaaditya.example.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.nantaaditya.example.properties.embedded.ClientConfiguration;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ClientPropertiesTest {

  @Test
  void getClientConfiguration_existingName_returnsConfiguration() {
    ClientConfiguration config = mock(ClientConfiguration.class);
    ClientProperties props = new ClientProperties(Map.of("service-a", config), null, null);

    ClientConfiguration result = props.getClientConfiguration("service-a");

    assertNotNull(result);
    assertEquals(config, result);
  }

  @Test
  void getClientConfiguration_unknownName_returnsNull() {
    ClientConfiguration config = mock(ClientConfiguration.class);
    ClientProperties props = new ClientProperties(Map.of("service-a", config), null, null);

    assertNull(props.getClientConfiguration("unknown"));
  }

  @Test
  void getBeanNames_withConfigurations_appendsPostfix() {
    ClientConfiguration config = mock(ClientConfiguration.class);
    ClientProperties props = new ClientProperties(Map.of("service-a", config), null, null);

    Set<String> names = props.getBeanNames("RestClient");

    assertEquals(Set.of("service-aRestClient"), names);
  }

  @Test
  void getBeanNames_nullConfigurations_returnsEmpty() {
    ClientProperties props = new ClientProperties(null, null, null);

    assertTrue(props.getBeanNames("RestClient").isEmpty());
  }

  @Test
  void getBeanNames_emptyConfigurations_returnsEmpty() {
    ClientProperties props = new ClientProperties(Map.of(), null, null);

    assertTrue(props.getBeanNames("RestClient").isEmpty());
  }

  @Test
  void getBeanNames_multipleClients_addsPostfixToAll() {
    ClientProperties props = new ClientProperties(
        Map.of("svc-a", mock(ClientConfiguration.class), "svc-b", mock(ClientConfiguration.class)),
        null, null);

    Set<String> names = props.getBeanNames("RestSender");

    assertEquals(Set.of("svc-aRestSender", "svc-bRestSender"), names);
  }
}
