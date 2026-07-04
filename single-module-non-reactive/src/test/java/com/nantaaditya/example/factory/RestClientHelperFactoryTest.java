package com.nantaaditya.example.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import com.nantaaditya.example.helper.RestClientHelper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RestClientHelperFactoryTest {

  private RestClientHelperFactory factory;

  @BeforeEach
  void setUp() {
    factory = new RestClientHelperFactory();
  }

  @Test
  void getObjectType_returnsRestClientHelperClass() {
    assertEquals(RestClientHelper.class, factory.getObjectType());
  }

  @Test
  void getObject_withEmptyMap_returnsHelper() throws Exception {
    assertNotNull(factory.getObject());
  }

  @Test
  void getRestClient_existingClient_returnsIt() throws Exception {
    RestClient mockClient = mock(RestClient.class);
    factory.setRestClients(Map.of("serviceARestClient", mockClient));

    RestClientHelper helper = factory.getObject();

    assertEquals(mockClient, helper.getRestClient("serviceA"));
  }

  @Test
  void getRestClient_unknownClient_returnsNull() throws Exception {
    factory.setRestClients(Map.of("serviceARestClient", mock(RestClient.class)));

    RestClientHelper helper = factory.getObject();

    assertNull(helper.getRestClient("unknown"));
  }

  @Test
  void getClientNames_stripsRestClientPostfix() throws Exception {
    factory.setRestClients(Map.of(
        "serviceARestClient", mock(RestClient.class),
        "serviceBRestClient", mock(RestClient.class)
    ));

    RestClientHelper helper = factory.getObject();

    assertEquals(2, helper.getClientNames().size());
    assertEquals(true, helper.getClientNames().contains("serviceA"));
    assertEquals(true, helper.getClientNames().contains("serviceB"));
  }
}
