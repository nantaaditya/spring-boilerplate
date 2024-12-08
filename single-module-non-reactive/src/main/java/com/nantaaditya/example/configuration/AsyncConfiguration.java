package com.nantaaditya.example.configuration;

import com.nantaaditya.example.properties.AsyncTaskProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public ThreadPoolTaskExecutor asyncTaskExecutor(AsyncTaskProperties asyncTaskProperties) {
    return createAsyncExecutor(asyncTaskProperties.getConfiguration("default"));
  }

  private ThreadPoolTaskExecutor createAsyncExecutor(AsyncTaskProperties.Configuration threadConfiguration) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(threadConfiguration.getCorePoolSize());
    executor.setMaxPoolSize(threadConfiguration.getMaxPoolSize());
    executor.setQueueCapacity(threadConfiguration.getQueueCapacity());
    executor.setThreadNamePrefix(threadConfiguration.getThreadNamePrefix());
    executor.setKeepAliveSeconds(threadConfiguration.getKeepAliveSeconds());
    executor.initialize();
    return executor;
  }
}
