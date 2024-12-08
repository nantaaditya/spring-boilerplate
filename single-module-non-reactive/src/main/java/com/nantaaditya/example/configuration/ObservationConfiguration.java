package com.nantaaditya.example.configuration;

import com.nantaaditya.example.listener.AppObservationListener;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservationConfiguration {

  @Bean
  public ObservationRegistry observationRegistry() {
    ObservationRegistry observationRegistry =  ObservationRegistry.create();
    observationRegistry
        .observationConfig()
        .observationHandler(new AppObservationListener());
    return observationRegistry;
  }

  @Bean
  public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
    return new ObservedAspect(observationRegistry);
  }
}
