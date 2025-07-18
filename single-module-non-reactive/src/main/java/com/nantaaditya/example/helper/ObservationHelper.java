package com.nantaaditya.example.helper;

import com.nantaaditya.example.model.constant.FeatureConstant;
import com.nantaaditya.example.model.dto.ContextDTO;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.Observation.Event;
import io.micrometer.observation.ObservationRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class ObservationHelper {

  private final ObservationRegistry observationRegistry;

  public Context createApiContext(ContextDTO contextDTO) {
    Context observationContext = new Context();

    FeatureConstant feature = FeatureConstant.get(contextDTO.method(), contextDTO.path());
    if (feature != null) {
      observationContext.addLowCardinalityKeyValue(KeyValue.of("feature", feature.name()));
    }

    if (contextDTO.requestId() != null) {
      observationContext.addHighCardinalityKeyValue(KeyValue.of("requestId", contextDTO.requestId()));
    }

    return observationContext;
  }

  public void publishEvent(String key, String value) {
    Observation observation = observationRegistry.getCurrentObservation();
    if (observation == null) {
      log.warn("#Observation - no current observation");
      return;
    }

    observation.event(Event.of(key, value));
  }
}

