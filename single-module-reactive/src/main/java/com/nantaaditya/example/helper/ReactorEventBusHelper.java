package com.nantaaditya.example.helper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitResult;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
public class ReactorEventBusHelper {

  private final Map<String, Sinks.Many> sinks = new ConcurrentHashMap<>();
  private final Lock lock = new ReentrantLock(true);
  private final long timeOut = 5l;
  private final TimeUnit timeOutTimeUnit = TimeUnit.SECONDS;

  public <T> void createSinks(String name, Sinks.Many<T> sink) {
    try {
      log.info("#Event - create sinks for {}", name);
      sinks.put(name, sink);
    } catch (Exception e) {
      log.error("#Event - create sinks {} error {}", name, e.getMessage());
    }
  }

  public <T> Sinks.Many<T> getSinks(String name) {
    return sinks.get(name);
  }

  public <T> void publish(String name, T event) {
    try {
      if (event == null || event == null) {
        log.warn("#Event - event payload is null");
        return;
      }

      lock.tryLock(timeOut, timeOutTimeUnit);

      Sinks.Many<T> sink = sinks.get(name);
      if (sink == null) {
        log.warn("#Event - sinks for {} not found", name);
        return;
      }

      EmitResult emitResult = sink.tryEmitNext(event);
      if (emitResult.isFailure()) {
        log.error("#Event - failed emit event {}, error {}", event, emitResult);
        emitResult.orThrow();
      }
    } catch (InterruptedException ex) {
      log.error("#Event - interrupted while waiting for sinks for {}, error {} cause", name, ex.getCause(), ErrorHelper.getRootCause(ex));
    } finally {
      lock.unlock();
    }
  }

  public <T> Flux<T> consume(String name, Scheduler scheduler) {
    if (scheduler == null) {
      scheduler = Schedulers.immediate();
    }

    Sinks.Many<T> sink = sinks.get(name);
    if (sink == null) {
      return Flux.empty();
    }

    return sink.asFlux()
        .doOnNext(event -> log.debug("#Event - consume sink for {} value {}", name, event))
        .publishOn(scheduler);
  }
}
