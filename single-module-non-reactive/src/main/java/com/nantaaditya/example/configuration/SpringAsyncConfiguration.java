package com.nantaaditya.example.configuration;

import com.nantaaditya.example.helper.AsyncMDCTaskDecorator;
import com.nantaaditya.example.helper.ExecutorHelper;
import com.nantaaditya.example.helper.ObservationWrapper;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.properties.AsyncTaskProperties;
import com.nantaaditya.example.properties.embedded.AsyncConfiguration;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import lombok.extern.log4j.Log4j2;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;

@Log4j2
@Configuration
public class SpringAsyncConfiguration implements AsyncConfigurer {

  @Autowired
  private AsyncTaskProperties asyncProperties;

  @Autowired
  private ObservationWrapper observationWrapper;

  @Value("${spring.threads.virtual.enabled:false}")
  private boolean virtualThreadEnabled;

  @Override
  public Executor getAsyncExecutor() {
    AsyncMDCTaskDecorator asyncMDCTaskDecorator = new AsyncMDCTaskDecorator(observationWrapper);
    AsyncConfiguration configuration = asyncProperties.getConfiguration("default");
    return ExecutorHelper.create(
        configuration.threadNamePrefix(),
        configuration.corePoolSize(),
        configuration.maxPoolSize(),
        configuration.queueCapacity(),
        configuration.keepAliveSeconds(),
        asyncMDCTaskDecorator,
        virtualThreadEnabled,
        new AbortPolicy()
    );
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return new AsyncUncaughtExceptionHandler() {
      @Override
      public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        log.error(AppLogMessage.message("#Async - got error {}, httpMethod {}, params {}",
            ex.getMessage(), method.getName(), params).error(ex));
      }
    };
  }
}
