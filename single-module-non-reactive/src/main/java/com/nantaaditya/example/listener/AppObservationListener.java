package com.nantaaditya.example.listener;

import com.nantaaditya.example.model.constant.ObservationConstant;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.properties.LogProperties;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.Observation.Event;
import io.micrometer.observation.ObservationHandler;
import java.util.LinkedHashMap;
import java.util.Map;
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
    log.info(AppLogMessage.message("#Metrics - start").additionalData(createContext(context)));
  }

  @Override
  public void onEvent(Event event, Context context) {
    log.info(AppLogMessage.message("#Metrics - event").additionalData(createContext(context)));
  }

  @Override
  public void onError(Context context) {
    log.error(AppLogMessage.message("#Metrics - error").additionalData(createContext(context)));
  }

  @Override
  public void onStop(Context context) {
    log.info(AppLogMessage.message("#Metrics - stop").additionalData(createContext(context)));
  }

  public static boolean isEligibleToObserved(Context context) {
    return Stream.of(ObservationConstant.values())
        .anyMatch(item -> item.getName().equals(context.getName()));
  }

  private Map<String, Object> createContext(Context context) {
    Map<String, Object> ctx = new LinkedHashMap<>();
    ctx.put("name", context.getName());
    ctx.put("lowCardinalityKV", context.getLowCardinalityKeyValues());
    ctx.put("highCardinalityKV", context.getHighCardinalityKeyValues());
    return ctx;
  }

}
