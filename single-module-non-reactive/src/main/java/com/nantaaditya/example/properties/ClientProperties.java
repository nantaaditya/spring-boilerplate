package com.nantaaditya.example.properties;

import com.nantaaditya.example.model.constant.ClientLogFormat;
import com.nantaaditya.example.properties.embedded.ClientConfiguration;
import com.nantaaditya.example.properties.embedded.ClientPoolingConfiguration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(value = "apps.client")
public record ClientProperties(
    Map<String, ClientConfiguration> configurations,
    ClientPoolingConfiguration pooling,
    ClientLogFormat logFormat
) {

  public ClientConfiguration getClientConfiguration(String clientName) {
    return configurations.get(clientName);
  }

  public Set<String> getBeanNames(String postfix) {
    Set<String> result = new HashSet<>();
    if (configurations == null || configurations.isEmpty()) return result;

    for (Map.Entry<String, ClientConfiguration> entry : configurations.entrySet()) {
      result.add(entry.getKey() + postfix);
    }
    return result;
  }

}
