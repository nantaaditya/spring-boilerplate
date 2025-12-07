package com.nantaaditya.example.listener;

import com.nantaaditya.example.model.constant.ObservationConstant;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.properties.LogProperties;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.Observation.Event;
import io.micrometer.observation.ObservationHandler;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class AppObservationListener implements ObservationHandler<Context> {

  private final LogProperties logProperties;

  @Override
  public boolean supportsContext(Context context) {
    return logProperties.enableMetricLog() && isEligibleToObserved(context);
  }

  @Override
  public void onStart(Context context) {
    log.info(AppLogMessage.create("#Metrics - start", context));
  }

  @Override
  public void onEvent(Event event, Context context) {
    log.info(AppLogMessage.create("#Metrics - event", event));
  }

  @Override
  public void onError(Context context) {
    log.error(AppLogMessage.create("#Metrics - error", context));
  }

  @Override
  public void onStop(Context context) {
    log.info(AppLogMessage.create("#Metrics - stop", context));
  }

  public static boolean isEligibleToObserved(Context context) {
    return Stream.of(ObservationConstant.values())
        .anyMatch(item -> item.getName().equals(context.getName()));
  }
}
