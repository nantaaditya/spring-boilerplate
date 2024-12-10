package com.nantaaditya.example.properties;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("apps.async")
@SuppressWarnings("java:S1068")
public class AsyncTaskProperties {

  private Map<String, Configuration> configurations = new HashMap<>();

  @Data
  public static class Configuration {
    private int corePoolSize;
    private int maxPoolSize;
    private int queueCapacity;
    private int keepAliveSeconds;
    private String threadNamePrefix;
  }

  public Configuration getConfiguration(String name) {
    return configurations.get(name);
  }
}
