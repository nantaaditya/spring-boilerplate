package com.nantaaditya.example.helper;

import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReactorHelper {

  private final TracerHelper tracerHelper;

  public <T> void runBackgroundTask(String processName, Supplier<Mono<T>> monoSupplier, Scheduler scheduler) {
    Mono.defer(() ->
        monoSupplier.get()
          .doOnNext(result -> tracerHelper.setBaggage("backgroundTask", processName))
      )
      .subscribeOn(scheduler)
      .subscribe(
          success -> log.info("#Reactor - run background task {} success", processName),
          error -> log.error("#Reactor - run background task {} error, error {} cause {}",
              processName, error.getMessage(), ErrorHelper.getRootCause(error))
      );
  }
}
