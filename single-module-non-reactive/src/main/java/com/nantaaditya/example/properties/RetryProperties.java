package com.nantaaditya.example.properties;

import com.nantaaditya.example.properties.embedded.RetryConfiguration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(value = "apps.retry")
public record RetryProperties(
    Map<String, RetryConfiguration> configurations
) {

  public RetryConfiguration get(String retryKey) {
    return configurations.get(retryKey);
  }

  public Set<String> getBeanNames(String postfix) {
    Set<String> result = new HashSet<>();
    if (configurations == null || configurations.isEmpty()) return result;

    for (Map.Entry<String, RetryConfiguration> entry : configurations.entrySet()) {
      result.add(entry.getKey() + postfix);
    }
    return result;
  }
}
