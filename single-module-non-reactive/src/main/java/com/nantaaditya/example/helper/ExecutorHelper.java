package com.nantaaditya.example.helper;

import java.util.concurrent.RejectedExecutionHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class ExecutorHelper {

  private ExecutorHelper() {}

  public static ThreadPoolTaskExecutor create(String threadNamePrefix, int corePoolSize,
      int maximumPoolSize, int queueCapacity, int keepAliveTime, AsyncMDCTaskDecorator taskDecorator,
      boolean virtualThreadEnabled, RejectedExecutionHandler rejectedExecutionHandler) {

    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maximumPoolSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setThreadNamePrefix(threadNamePrefix);
    executor.setKeepAliveSeconds(keepAliveTime);
    executor.setTaskDecorator(taskDecorator);
    executor.setRejectedExecutionHandler(rejectedExecutionHandler);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setVirtualThreads(virtualThreadEnabled);
    executor.initialize();
    return executor;
  }
}
