package com.nantaaditya.example.helper;

import com.nantaaditya.example.model.constant.AppsFeatureConstant;
import com.nantaaditya.example.model.constant.ResponseCode;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.model.dto.ContextDTO;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.Observation.Event;
import io.micrometer.observation.ObservationRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Log4j2
@Getter
@Component
@RequiredArgsConstructor
public class ObservationHelper {

  private final ObservationRegistry observationRegistry;

  private static final String ERROR_KEY = "error";
  private static final String RESPONSE_CODE = "responseCode";

  public Context createApiContext(ContextDTO contextDTO) {
    Context observationContext = new Context();

    AppsFeatureConstant feature = AppsFeatureConstant.get(contextDTO.method(), contextDTO.path());
    if (feature != null) {
      observationContext.addLowCardinalityKeyValue(KeyValue.of("feature", feature.name()));
    } else {
      observationContext.addLowCardinalityKeyValue(KeyValue.of("feature", contextDTO.getUnknownFeature()));
    }

    if (contextDTO.requestId() != null) {
      observationContext.addHighCardinalityKeyValue(KeyValue.of("requestId", contextDTO.requestId()));
    }

    return observationContext;
  }

  public void publishEvent(Observation observation,String key, String value) {
    if (observation == null) {
      log.warn(AppLogMessage.message("#Observation - no current observation"));
      return;
    }

    observation.event(Event.of(key, value));
  }

  public void decorateErrorObservation(ObservationWrapper observationWrapper, Throwable throwable, ResponseCode responseCode) {
    Observation observation = observationWrapper.getObservation();
    if (observation != null) {
      if (responseCode != null) {
        observation.lowCardinalityKeyValue(RESPONSE_CODE, responseCode.name());
      }

      if (throwable != null) {
        String exceptionClass = throwable.getClass().getName();
        observation.lowCardinalityKeyValue(ERROR_KEY, exceptionClass);
        publishEvent(observation, ERROR_KEY, exceptionClass);
        observation.error(throwable);
      }
    }
  }

}

