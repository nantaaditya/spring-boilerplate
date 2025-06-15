package com.nantaaditya.example.factory.impl;

import com.nantaaditya.example.factory.WebClientFactory;
import com.nantaaditya.example.factory.WebClientServiceFactory;
import com.nantaaditya.example.properties.ClientProperties;
import com.nantaaditya.example.service.WebClientService;
import com.nantaaditya.example.service.impl.WebClientServiceImpl;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.FactoryBean;

@Slf4j
@Setter
public class WebClientServiceFactoryBean implements FactoryBean<WebClientServiceFactory> {

  private ClientProperties clientProperties;
  private WebClientFactory webClientFactory;

  @Override
  public Class<?> getObjectType() {
    return WebClientServiceFactory.class;
  }

  @Override
  public WebClientServiceFactory getObject() throws Exception {
    if (clientProperties.configurations() == null) {
      return new WebClientServiceFactoryImpl(Collections.emptyMap());
    }

    Map<String, WebClientService> configurations = clientProperties
        .configurations()
        .entrySet()
        .stream()
        .peek(item -> log.debug("#WebClient - initializing {}", item.getKey()))
        .collect(Collectors.toMap(
            Entry::getKey,
            entry -> new WebClientServiceImpl(entry.getKey(), webClientFactory)
        ));
    return new WebClientServiceFactoryImpl(configurations);
  }
}