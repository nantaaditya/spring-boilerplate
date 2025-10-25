package com.nantaaditya.example.helper;

import com.nantaaditya.example.properties.embedded.AsyncConfiguration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskRetryHelper {

  private final BlockingQueue<Runnable> retryQueue;
  private final Executor executor;

  public TaskRetryHelper(AsyncMDCTaskDecorator asyncMDCTaskDecorator,
      AsyncConfiguration asyncConfiguration, int maxRetryRejectedTask) {
    this.retryQueue = new LinkedBlockingQueue<>(maxRetryRejectedTask);
    this.executor = createExecutor(asyncConfiguration, asyncMDCTaskDecorator);
    runRejectedTask();
  }

  private Executor createExecutor(AsyncConfiguration configuration,
      AsyncMDCTaskDecorator asyncMDCTaskDecorator) {
    return ExecutorHelper.create(
        configuration.threadNamePrefix(),
        configuration.corePoolSize(),
        configuration.maxPoolSize(),
        configuration.queueCapacity(),
        configuration.keepAliveSeconds(),
        asyncMDCTaskDecorator,
        new CallerRunsPolicy()
    );
  }

  public void enqueue(Runnable task) {
    try {
      boolean success = retryQueue.offer(task);
      if (!success) {
        log.error("#RetryExecutor - retry queue full, dropping task.");
      }
    } catch (Exception e) {
      log.error("#RetryExecutor - failed to re-enqueue task, error: {},", e.getMessage(), e);
    }
  }

  private void runRejectedTask() {
    Thread retryThread = new Thread(() -> {
      while (true) {
        try {
          Runnable task = retryQueue.take();
          try {
            executor.execute(task);
          } catch (RejectedExecutionException e) {
            log.error("#RetryExecutor is full — re-queueing task");
            Thread.sleep(1000);
            enqueue(task);
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    });

    retryThread.setDaemon(true);
    retryThread.setName("Retry-Rejected-Worker");
    retryThread.start();
  }
}
