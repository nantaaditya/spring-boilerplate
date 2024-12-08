package com.nantaaditya.example.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(value = "apps.swagger")
public class SwaggerProperties {
  private String host;
}
