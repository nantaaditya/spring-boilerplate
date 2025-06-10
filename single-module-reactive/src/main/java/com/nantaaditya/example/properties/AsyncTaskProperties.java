package com.nantaaditya.example.properties;

import com.nantaaditya.example.properties.embedded.AsyncConfiguration;
import java.beans.Transient;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("apps.async")
public record AsyncTaskProperties(
    Map<String, AsyncConfiguration> configurations,
    AsyncConfiguration retryRejectedTask,
    int maxRetryRejectedTask
) {

  @Transient
  public AsyncConfiguration getConfiguration(String name) {
    return configurations.get(name);
  }
}
