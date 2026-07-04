package com.nantaaditya.example.helper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.observation.Observation;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ObservationWrapperTest {

  private ObservationWrapper observationWrapper;

  @BeforeEach
  void setUp() {
    observationWrapper = new ObservationWrapper();
  }

  @Test
  void setObservation_nullRequest_doesNotThrow() {
    Observation observation = mock(Observation.class);
    observationWrapper.setObservation(null, observation);
  }

  @Test
  void setObservation_storesAttributeOnRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    Observation observation = mock(Observation.class);
    observationWrapper.setObservation(request, observation);
    assertNotNull(observationWrapper.getObservation(request));
  }

  @Test
  void getObservation_nullRequest_returnsNull() {
    assertNull(observationWrapper.getObservation(null));
  }

  @Test
  void getObservation_noAttribute_returnsNull() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    assertNull(observationWrapper.getObservation(request));
  }

  @Test
  void getObservation_returnsStoredObservation() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    Observation observation = mock(Observation.class);
    observationWrapper.setObservation(request, observation);
    assertNotNull(observationWrapper.getObservation(request));
  }

  @Test
  void clear_nullRequest_doesNotThrow() {
    observationWrapper.clear(null);
  }

  @Test
  void clear_removesAttribute() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    Observation observation = mock(Observation.class);
    observationWrapper.setObservation(request, observation);
    observationWrapper.clear(request);
    assertNull(observationWrapper.getObservation(request));
  }

  @Test
  void wrap_runnable_executes() {
    AtomicBoolean executed = new AtomicBoolean(false);
    Runnable wrapped = observationWrapper.wrap(() -> executed.set(true));
    wrapped.run();
    assertTrue(executed.get());
  }

  @Test
  void wrap_runnableWithRequest_propagatesObservation() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    Observation observation = mock(Observation.class);
    Observation.Scope scope = mock(Observation.Scope.class);
    when(observation.openScope()).thenReturn(scope);
    observationWrapper.setObservation(request, observation);

    AtomicBoolean executed = new AtomicBoolean(false);
    Runnable wrapped = observationWrapper.wrap(() -> executed.set(true), request);
    wrapped.run();

    assertTrue(executed.get());
    verify(observation).openScope();
    verify(scope).close();
  }

  @Test
  void wrap_runnableWithRequest_nullObservation_stillExecutes() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    AtomicBoolean executed = new AtomicBoolean(false);
    Runnable wrapped = observationWrapper.wrap(() -> executed.set(true), request);
    wrapped.run();
    assertTrue(executed.get());
  }
}
