package com.nantaaditya.example.helper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nantaaditya.example.model.constant.ResponseCode;
import com.nantaaditya.example.model.dto.ContextDTO;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObservationHelperTest {

  @InjectMocks
  private ObservationHelper observationHelper;

  @Mock
  private ObservationRegistry observationRegistry;

  @Test
  void createApiContext_knownFeature() {
    // POST /api/card matches FeatureConstant.ISSUANCE_CARD
    ContextDTO contextDTO = new ContextDTO("clientId", "req-001", "POST", "/api/card",
        null, null, null, null, null);
    Context context = observationHelper.createApiContext(contextDTO);
    assertNotNull(context);
  }

  @Test
  void createApiContext_unknownFeature() {
    ContextDTO contextDTO = new ContextDTO("clientId", "req-001", "GET", "/api/unknown-path",
        null, null, null, null, null);
    Context context = observationHelper.createApiContext(contextDTO);
    assertNotNull(context);
  }

  @Test
  void createApiContext_nullRequestId() {
    ContextDTO contextDTO = new ContextDTO("clientId", null, "GET", "/api/unknown-path",
        null, null, null, null, null);
    Context context = observationHelper.createApiContext(contextDTO);
    assertNotNull(context);
  }

  @Test
  void publishEvent_nullObservation() {
    observationHelper.publishEvent(null, "key", "value");
  }

  @Test
  void publishEvent_validObservation() {
    Observation observation = mock(Observation.class);
    observationHelper.publishEvent(observation, "key", "value");
    verify(observation).event(any(Observation.Event.class));
  }

  @Test
  void decorateResponseObservation_nullObservation() {
    observationHelper.decorateResponseObservation(null, null, ResponseCode.SUCCESS);
  }

  @Test
  void decorateResponseObservation_responseCodeOnly() {
    Observation observation = mock(Observation.class);
    observationHelper.decorateResponseObservation(observation, null, ResponseCode.SUCCESS);
    verify(observation).lowCardinalityKeyValue("responseCode", ResponseCode.SUCCESS.name());
    verify(observation, never()).error(any());
  }

  @Test
  void decorateResponseObservation_withThrowable() {
    Observation observation = mock(Observation.class);
    RuntimeException throwable = new RuntimeException("test error");
    observationHelper.decorateResponseObservation(observation, throwable, ResponseCode.SUCCESS);
    verify(observation).lowCardinalityKeyValue("responseCode", ResponseCode.SUCCESS.name());
    verify(observation).lowCardinalityKeyValue("error", throwable.getClass().getName());
    verify(observation).error(throwable);
  }

  @Test
  void decorateResponseObservation_httpStatusOnly_nullObservation() {
    observationHelper.decorateResponseObservation(null, "404");
  }

  @Test
  void decorateResponseObservation_httpStatusOnly_setsStatusWithoutError() {
    Observation observation = mock(Observation.class);
    observationHelper.decorateResponseObservation(observation, "404");
    verify(observation).lowCardinalityKeyValue("responseCode", "404");
    verify(observation, never()).error(any());
  }

  @Test
  void decorateResponseObservation_httpStatusString_withThrowable_marksError() {
    Observation observation = mock(Observation.class);
    RuntimeException throwable = new RuntimeException("connection reset");
    observationHelper.decorateResponseObservation(observation, throwable, "500");
    verify(observation).lowCardinalityKeyValue("responseCode", "500");
    verify(observation).lowCardinalityKeyValue("error", throwable.getClass().getName());
    verify(observation).error(throwable);
  }

  @Test
  void decorateResponseObservation_nullHttpStatusAndThrowable_noInteractions() {
    Observation observation = mock(Observation.class);
    observationHelper.decorateResponseObservation(observation, null, (String) null);
    verify(observation, never()).lowCardinalityKeyValue(any(), any());
    verify(observation, never()).error(any());
  }
}
