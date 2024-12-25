package com.nantaaditya.example.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nantaaditya.example.factory.RetryTemplateHelperFactory;
import com.nantaaditya.example.listener.RetryTemplateListener;
import com.nantaaditya.example.properties.RetryProperties;
import com.nantaaditya.example.properties.embedded.RetryConfiguration;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.backoff.UniformRandomBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RetryTemplateConfiguration {

  private final ObjectMapper objectMapper;
  private final DeadLetterProcessRepository deadLetterProcessRepository;
  private final RetryProperties retryProperties;

  private static final String POSTFIX_BEAN_NAME = "RetryTemplate";

  @Bean
  public RetryTemplateHelperFactory retryTemplateHelperFactory() {
    Map<String, RetryTemplate> retryTemplates = new HashMap<>();

    RetryTemplateHelperFactory factory = new RetryTemplateHelperFactory();
    if (retryProperties.configurations() == null || retryProperties.configurations().isEmpty()) {
      log.warn("#Retry - no bean defined");
      factory.setRetryTemplates(retryTemplates);
      return factory;
    }

    retryTemplates.putAll(retryProperties.configurations()
        .entrySet()
        .stream()
        .collect(Collectors.toMap(
            e -> e.getKey() + POSTFIX_BEAN_NAME,
            e -> createRetryTemplate(e.getKey(), retryProperties.get(e.getKey()))
        ))
    );
    factory.setRetryTemplates(retryTemplates);
    log.debug("#Retry - [{}] created", retryProperties.getBeanNames(POSTFIX_BEAN_NAME));
    return factory;
  }

  public RetryTemplate createRetryTemplate(String name, RetryConfiguration configuration) {
    RetryTemplate retryTemplate = new RetryTemplate();
    setPolicy(retryTemplate, configuration);

    SimpleRetryPolicy retryPolicy = configuration.getRetryableExceptionMaps().isEmpty() ?
        new SimpleRetryPolicy(configuration.maxAttempt())
        : new SimpleRetryPolicy(configuration.maxAttempt(), configuration.getRetryableExceptionMaps());

    retryTemplate.setRetryPolicy(retryPolicy);
    retryTemplate.registerListener(new RetryTemplateListener(name, objectMapper, deadLetterProcessRepository));
    return retryTemplate;
  }

  private void setPolicy(RetryTemplate retryTemplate, RetryConfiguration configuration) {
    switch (configuration.type()) {
      case FIXED -> {
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(configuration.initialInterval());
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
      }
      case EXPONENTIAL -> {
        ExponentialBackOffPolicy exponentialBackOffPolicy = new ExponentialBackOffPolicy();
        exponentialBackOffPolicy.setInitialInterval(configuration.initialInterval());
        exponentialBackOffPolicy.setMultiplier(configuration.multiplier());
        exponentialBackOffPolicy.setMaxInterval(configuration.maxInterval());
        retryTemplate.setBackOffPolicy(exponentialBackOffPolicy);
      }
      case UNIFORM_RANDOM -> {
        UniformRandomBackOffPolicy uniformRandomBackOffPolicy = new UniformRandomBackOffPolicy();
        uniformRandomBackOffPolicy.setMinBackOffPeriod(configuration.initialInterval());
        uniformRandomBackOffPolicy.setMaxBackOffPeriod(configuration.maxInterval());
        retryTemplate.setBackOffPolicy(uniformRandomBackOffPolicy);
      }
      case EXPONENTIAL_RANDOM -> {
        ExponentialRandomBackOffPolicy exponentialRandomBackOffPolicy = new ExponentialRandomBackOffPolicy();
        exponentialRandomBackOffPolicy.setInitialInterval(configuration.initialInterval());
        exponentialRandomBackOffPolicy.setMultiplier(configuration.multiplier());
        exponentialRandomBackOffPolicy.setMaxInterval(configuration.maxInterval());
        retryTemplate.setBackOffPolicy(exponentialRandomBackOffPolicy);
      }
    }
  }


}
