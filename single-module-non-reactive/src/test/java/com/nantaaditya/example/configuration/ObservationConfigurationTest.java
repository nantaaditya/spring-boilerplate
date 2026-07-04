package com.nantaaditya.example.configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.nantaaditya.example.listener.AppObservationListener;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObservationConfigurationTest {

  @InjectMocks
  private ObservationConfiguration configuration;

  @Mock
  private AppObservationListener observationListener;

  private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

  @Test
  void observationRegistry_returnsNonNull() {
    assertNotNull(configuration.observationRegistry(observationListener, meterRegistry));
  }

  @Test
  void observedAspect_returnsNonNull() {
    ObservationRegistry registry = configuration.observationRegistry(observationListener, meterRegistry);

    assertNotNull(configuration.observedAspect(registry));
  }

  @Test
  void currentTraceContext_returnsNonNull() {
    assertNotNull(configuration.currentTraceContext());
  }
}
