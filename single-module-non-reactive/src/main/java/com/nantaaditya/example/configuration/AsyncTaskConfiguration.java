package com.nantaaditya.example.configuration;

import com.nantaaditya.example.helper.AsyncMDCTaskDecorator;
import com.nantaaditya.example.properties.AsyncTaskProperties;
import com.nantaaditya.example.properties.embedded.AsyncConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.support.GenericWebApplicationContext;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AsyncTaskConfiguration {

  private final AsyncTaskProperties asyncProperties;
  private final GenericWebApplicationContext applicationContext;

  private static final String POSTFIX_BEAN_NAME = "AsyncTaskExecutor";

  @EventListener(ApplicationReadyEvent.class)
  public void onStart() {
    if (asyncProperties.configurations() == null || asyncProperties.configurations().isEmpty()) {
      log.warn("#AsyncExecutor - no bean defined");
      return;
    }

    AsyncMDCTaskDecorator asyncMDCTaskDecorator = new AsyncMDCTaskDecorator();
    asyncProperties.configurations()
        .forEach((key, value) -> applicationContext.registerBean(
                key + POSTFIX_BEAN_NAME,
                ThreadPoolTaskExecutor.class,
                () -> createAsyncExecutor(asyncProperties.getConfiguration(key), asyncMDCTaskDecorator),
                definition -> definition.setLazyInit(true)
            )
        );

    log.debug("#AsyncExecutor - bean {} created", asyncProperties.getConfiguration(POSTFIX_BEAN_NAME));
  }

  private ThreadPoolTaskExecutor createAsyncExecutor(AsyncConfiguration configuration,
      AsyncMDCTaskDecorator asyncMDCTaskDecorator) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(configuration.corePoolSize());
    executor.setMaxPoolSize(configuration.maxPoolSize());
    executor.setQueueCapacity(configuration.queueCapacity());
    executor.setThreadNamePrefix(configuration.threadNamePrefix());
    executor.setKeepAliveSeconds(configuration.keepAliveSeconds());
    executor.setTaskDecorator(asyncMDCTaskDecorator);
    executor.initialize();
    return executor;
  }
}