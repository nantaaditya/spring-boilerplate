package com.nantaaditya.example.listener;

import com.nantaaditya.example.model.constant.ObservationConstant;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.Observation.Event;
import io.micrometer.observation.ObservationHandler;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AppObservationListener implements ObservationHandler<Context> {

  @Override
  public boolean supportsContext(Context context) {
    return true;
  }

  @Override
  public void onStart(Context context) {
    if (!isEligibleToObserved(context)) {
      return;
    }
    log.info("#Metrics - start {}", context);
  }

  @Override
  public void onEvent(Event event, Context context) {
    if (!isEligibleToObserved(context)) {
      return;
    }
    log.info("#Metrics - event {}", event);
  }

  @Override
  public void onError(Context context) {
    if (!isEligibleToObserved(context)) {
      return;
    }
    log.error("#Metrics - error {}", context);
  }

  @Override
  public void onStop(Context context) {
    if (!isEligibleToObserved(context)) {
      return;
    }
    log.info("#Metrics - stop {}", context);
  }

  public static boolean isEligibleToObserved(Context context) {
    return Stream.of(ObservationConstant.values())
        .anyMatch(item -> item.getName().equals(context.getName()));
  }
}
