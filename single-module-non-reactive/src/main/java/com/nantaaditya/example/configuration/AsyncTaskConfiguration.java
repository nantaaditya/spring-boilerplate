package com.nantaaditya.example.configuration;

import com.nantaaditya.example.helper.AsyncMDCTaskDecorator;
import com.nantaaditya.example.helper.ExecutorHelper;
import com.nantaaditya.example.helper.ObservationWrapper;
import com.nantaaditya.example.helper.TaskRetryHelper;
import com.nantaaditya.example.properties.AsyncTaskProperties;
import com.nantaaditya.example.properties.embedded.AsyncConfiguration;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
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
  private final ObservationWrapper observationWrapper;

  private static final String POSTFIX_BEAN_NAME = "AsyncTaskExecutor";

  @EventListener(ApplicationReadyEvent.class)
  public void onStart() {
    if (asyncProperties.configurations() == null || asyncProperties.configurations().isEmpty()) {
      log.warn("#AsyncExecutor - no bean defined");
      return;
    }

    AsyncMDCTaskDecorator asyncMDCTaskDecorator = new AsyncMDCTaskDecorator(observationWrapper);
    TaskRetryHelper taskRetryHelper = new TaskRetryHelper(asyncMDCTaskDecorator,
        asyncProperties.retryRejectedTask(), asyncProperties.maxRetryRejectedTask());
    asyncProperties.configurations()
        .forEach((key, value) -> applicationContext.registerBean(
                key + POSTFIX_BEAN_NAME,
                ThreadPoolTaskExecutor.class,
                () -> createAsyncExecutor(asyncProperties.getConfiguration(key), asyncMDCTaskDecorator, taskRetryHelper),
                definition -> definition.setLazyInit(true)
            )
        );

    log.debug("#AsyncExecutor - bean {} created", asyncProperties.getConfiguration(POSTFIX_BEAN_NAME));
  }

  private ThreadPoolTaskExecutor createAsyncExecutor(AsyncConfiguration configuration,
      AsyncMDCTaskDecorator asyncMDCTaskDecorator, TaskRetryHelper taskRetryHelper) {
    return ExecutorHelper.create(
        configuration.threadNamePrefix(),
        configuration.corePoolSize(),
        configuration.maxPoolSize(),
        configuration.queueCapacity(),
        configuration.keepAliveSeconds(),
        asyncMDCTaskDecorator,
        new RejectedExecutionHandler() {
          @Override
          public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
            log.warn("#AsyncExecutor - rejected execution of task {}", task);
            taskRetryHelper.enqueue(task);
          }
        }
    );
  }
}