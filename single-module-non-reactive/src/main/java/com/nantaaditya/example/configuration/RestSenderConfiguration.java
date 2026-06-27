package com.nantaaditya.example.configuration;

import com.nantaaditya.example.factory.RestSenderFactory;
import com.nantaaditya.example.helper.RestClientHelper;
import com.nantaaditya.example.helper.RestSender;
import com.nantaaditya.example.helper.RetryTemplateHelper;
import com.nantaaditya.example.properties.RetryProperties;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
@AutoConfigureAfter(value = {RestClientBeanConfiguration.class, RetryTemplateConfiguration.class})
public class RestSenderConfiguration {

  private static final String POSTFIX_BEAN_NAME = "RestSender";

  @Value("${spring.application.name}")
  private String clientId;

  @Bean
  public RestSenderFactory restSenderFactory(RestClientHelper restClientHelper,
      RetryTemplateHelper retryTemplateHelper, RetryProperties retryProperties,
      ObjectMapper objectMapper) {
    RestSenderFactory restSenderFactory = new RestSenderFactory();

    Set<String> keys = new HashSet<>(restClientHelper.getClientNames());

    Map<String, RestSender> restSenders = new HashMap<>();
    for (String key : keys) {
      String beanName = key + POSTFIX_BEAN_NAME;
      restSenders.put(beanName, new RestSender.Builder(clientId, key, restClientHelper.getRestClient(key), objectMapper)
          .retryTemplate(retryTemplateHelper.getRetryTemplate(key))
          .retryConfiguration(retryProperties.get(key))
          .build()
      );
      restSenderFactory.setRestSenders(restSenders);
    }
    return restSenderFactory;
  }
}
