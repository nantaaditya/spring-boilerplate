package com.nantaaditya.example.configuration;

import com.nantaaditya.example.factory.RetryTemplateHelperFactory;
import com.nantaaditya.example.helper.RetryExhaustionNotifier;
import com.nantaaditya.example.listener.RetryTemplateListener;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.properties.RetryProperties;
import com.nantaaditya.example.properties.embedded.RetryConfiguration;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import com.nantaaditya.example.strategy.retry.ExponentialRandomBackOffPolicy;
import com.nantaaditya.example.strategy.retry.UniformRandomBackOffPolicy;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryPolicy.Builder;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.databind.ObjectMapper;

@Log4j2
@Configuration
@RequiredArgsConstructor
public class RetryTemplateConfiguration {

  private final ObjectMapper objectMapper;
  private final DeadLetterProcessRepository deadLetterProcessRepository;
  private final RetryProperties retryProperties;
  private final ObservationRegistry observationRegistry;
  private final MeterRegistry meterRegistry;
  private final RetryExhaustionNotifier retryExhaustionNotifier;

  private static final String POSTFIX_BEAN_NAME = "RetryTemplate";

  @Bean
  public RetryTemplateHelperFactory retryTemplateHelperFactory() {
    Map<String, RetryTemplate> retryTemplates = new HashMap<>();

    RetryTemplateHelperFactory factory = new RetryTemplateHelperFactory();
    if (retryProperties.configurations() == null || retryProperties.configurations().isEmpty()) {
      log.warn(AppLogMessage.message("#Retry - no bean defined"));
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
    log.debug(AppLogMessage.message("#Retry - [{}] created", retryProperties.getBeanNames(POSTFIX_BEAN_NAME)));
    return factory;
  }

  public RetryTemplate createRetryTemplate(String name, RetryConfiguration configuration) {
    RetryPolicy retryPolicy = createPolicy(configuration);

    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setRetryPolicy(retryPolicy);
    retryTemplate.setRetryListener(new RetryTemplateListener(name, objectMapper,
        deadLetterProcessRepository, observationRegistry, meterRegistry, retryExhaustionNotifier));
    return retryTemplate;
  }

  private RetryPolicy createPolicy(RetryConfiguration configuration) {
    RetryPolicy.Builder builder = RetryPolicy.builder();
    builder = createBuilder(configuration, builder);

    List<Class<? extends Throwable>> whitelistedExceptions = configuration.getWhitelistedExceptions();
    if (!whitelistedExceptions.isEmpty()) {
      builder = builder
          .includes(whitelistedExceptions);
    }

    List<Class<? extends Throwable>> blacklistedExceptions = configuration.getBlacklistedExceptions();
    if (!blacklistedExceptions.isEmpty()) {
      builder = builder
          .excludes(blacklistedExceptions);
    }

    return builder.build();
  }

  private RetryPolicy.Builder createBuilder(RetryConfiguration configuration, Builder builder) {
    return switch (configuration.type()) {
      case FIXED -> {
        BackOff fixedBackOff = new FixedBackOff(configuration.initialInterval(), configuration.maxAttempt());
        builder = builder.backOff(fixedBackOff);
        yield builder;
      }
      case EXPONENTIAL -> {
        ExponentialBackOff exponentialBackOffPolicy = new ExponentialBackOff(configuration.initialInterval(), configuration.multiplier());
        exponentialBackOffPolicy.setMaxInterval(configuration.maxInterval());
        exponentialBackOffPolicy.setMaxAttempts(configuration.maxAttempt());
        builder = builder.backOff(exponentialBackOffPolicy);
        yield builder;
      }
      case UNIFORM_RANDOM -> {
        UniformRandomBackOffPolicy uniformRandomBackOffPolicy = new UniformRandomBackOffPolicy(
            configuration.initialInterval(), configuration.maxInterval(), configuration.maxAttempt());
        builder = builder.backOff(uniformRandomBackOffPolicy);
        yield builder;
      }
      case EXPONENTIAL_RANDOM -> {
        ExponentialRandomBackOffPolicy exponentialRandomBackOffPolicy = new ExponentialRandomBackOffPolicy(
            configuration.initialInterval(), configuration.multiplier(), configuration.maxInterval(), configuration.maxAttempt()
        );

        builder = builder.backOff(exponentialRandomBackOffPolicy);
        yield builder;
      }
    };
  }
}
