package com.nantaaditya.example.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(value = "apps.swagger")
public record SwaggerProperties(
    String host
) {
}
