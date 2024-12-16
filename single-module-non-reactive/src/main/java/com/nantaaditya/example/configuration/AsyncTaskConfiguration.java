package com.nantaaditya.example.configuration;

import com.nantaaditya.example.properties.AsyncTaskProperties;
import com.nantaaditya.example.properties.embedded.AsyncConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncTaskConfiguration {

  @Bean("defaultAsyncTaskExecutor")
  @ConditionalOnMissingBean(TaskExecutor.class)
  public ThreadPoolTaskExecutor defaultAsyncTaskExecutor(AsyncTaskProperties asyncTaskProperties) {
    return createAsyncExecutor(asyncTaskProperties.getConfiguration("default"));
  }

  private ThreadPoolTaskExecutor createAsyncExecutor(AsyncConfiguration configuration) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(configuration.corePoolSize());
    executor.setMaxPoolSize(configuration.maxPoolSize());
    executor.setQueueCapacity(configuration.queueCapacity());
    executor.setThreadNamePrefix(configuration.threadNamePrefix());
    executor.setKeepAliveSeconds(configuration.keepAliveSeconds());
    executor.initialize();
    return executor;
  }
}
