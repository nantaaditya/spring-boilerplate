package com.nantaaditya.example.configuration;

import com.nantaaditya.example.helper.AsyncMDCTaskDecorator;
import com.nantaaditya.example.helper.DeadLetterRejectedExecutionHandler;
import com.nantaaditya.example.helper.ExecutorHelper;
import com.nantaaditya.example.helper.ObservationWrapper;
import com.nantaaditya.example.model.constant.AsyncRejectedStrategy;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.properties.AsyncTaskProperties;
import com.nantaaditya.example.properties.embedded.AsyncConfiguration;
import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import java.util.concurrent.RejectedExecutionHandler;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.support.GenericWebApplicationContext;

@Log4j2
@Configuration
public class AsyncTaskConfiguration {

  @Autowired
  private AsyncTaskProperties asyncProperties;
  @Autowired
  private GenericWebApplicationContext applicationContext;
  @Autowired
  private ObservationWrapper observationWrapper;
  @Autowired
  private DeadLetterProcessRepository deadLetterProcessRepository;
  @Value("${spring.threads.virtual.enabled:false}")
  private boolean virtualThreadEnabled;

  private static final String POSTFIX_BEAN_NAME = "AsyncTaskExecutor";

  @EventListener(ApplicationReadyEvent.class)
  public void onStart() {
    if (asyncProperties.configurations() == null || asyncProperties.configurations().isEmpty()) {
      log.warn(AppLogMessage.message("#AsyncExecutor - no bean defined"));
      return;
    }

    AsyncMDCTaskDecorator asyncMDCTaskDecorator = new AsyncMDCTaskDecorator(observationWrapper);
    asyncProperties.configurations()
        .forEach((key, value) -> applicationContext.registerBean(
                key + POSTFIX_BEAN_NAME,
                ThreadPoolTaskExecutor.class,
                () -> createAsyncExecutor(asyncProperties.getConfiguration(key), asyncMDCTaskDecorator),
                definition -> definition.setLazyInit(true)
            )
        );

    log.debug(AppLogMessage.message("#AsyncExecutor - beans created for {}", asyncProperties.configurations().keySet()));
  }

  private ThreadPoolTaskExecutor createAsyncExecutor(AsyncConfiguration configuration,
      AsyncMDCTaskDecorator asyncMDCTaskDecorator) {
    return ExecutorHelper.create(
        configuration.threadNamePrefix(),
        configuration.corePoolSize(),
        configuration.maxPoolSize(),
        configuration.queueCapacity(),
        configuration.keepAliveSeconds(),
        asyncMDCTaskDecorator,
        virtualThreadEnabled,
        buildRejectedHandler(configuration.rejectedTaskStrategy())
    );
  }

  private RejectedExecutionHandler buildRejectedHandler(AsyncRejectedStrategy strategy) {
    if (strategy == AsyncRejectedStrategy.DEAD_LETTER) {
      return new DeadLetterRejectedExecutionHandler(deadLetterProcessRepository);
    }
    return (task, executor) ->
        log.warn(AppLogMessage.message("#AsyncExecutor - task rejected and dropped [{}]", task.getClass().getSimpleName()));
  }
}