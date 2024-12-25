package com.nantaaditya.example.configuration;

import com.nantaaditya.example.factory.RestSenderFactory;
import com.nantaaditya.example.helper.RestClientHelper;
import com.nantaaditya.example.helper.RestSender;
import com.nantaaditya.example.helper.RetryTemplateHelper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@AutoConfigureAfter(value = {ClientBeanConfiguration.class, RetryTemplateConfiguration.class})
public class RestSenderConfiguration {

  private static final String POSTFIX_BEAN_NAME = "RestSender";

  @Bean
  public RestSenderFactory restSenderFactory(RestClientHelper restClientHelper,
      RetryTemplateHelper retryTemplateHelper) {
    RestSenderFactory restSenderFactory = new RestSenderFactory();

    Set<String> keys = new HashSet<>();
    keys.addAll(restClientHelper.getClientNames());

    Map<String, RestSender> restSenders = new HashMap<>();
    for (String key : keys) {
      String beanName = key + POSTFIX_BEAN_NAME;
      restSenders.put(beanName, new RestSender.Builder()
          .name(key)
          .restClient(restClientHelper.getRestClient(key))
          .retryTemplate(retryTemplateHelper.getRetryTemplate(key))
          .build()
      );
      restSenderFactory.setRestSenders(restSenders);
    }
    return restSenderFactory;
  }
}
