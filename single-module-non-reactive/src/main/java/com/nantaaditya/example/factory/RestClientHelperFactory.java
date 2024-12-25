package com.nantaaditya.example.factory;

import com.nantaaditya.example.helper.RestClientHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.web.client.RestClient;

@Setter
public class RestClientHelperFactory implements FactoryBean<RestClientHelper> {

  private Map<String, RestClient> restClients = new HashMap<>();

  @Override
  public RestClientHelper getObject() throws Exception {
    return new RestClientHelperImpl(restClients);
  }

  @Override
  public Class<?> getObjectType() {
    return RestClientHelper.class;
  }

  @AllArgsConstructor
  private static class RestClientHelperImpl implements RestClientHelper {
    private static final String POSTFIX_BEAN_NAME = "RestClient";

    private Map<String, RestClient> restClients = new HashMap<>();

    @Override
    public RestClient getRestClient(String clientName) {
      return restClients.get(clientName + POSTFIX_BEAN_NAME);
    }

    @Override
    public Set<String> getClientNames() {
      return restClients.entrySet()
          .stream()
          .map(e -> e.getKey().replace(POSTFIX_BEAN_NAME, ""))
          .collect(Collectors.toSet());
    }
  }
}
