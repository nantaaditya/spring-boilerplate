package com.nantaaditya.example.model.observation;

import io.micrometer.observation.Observation;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

/**
 * Carries retry-specific metadata through the Micrometer observation pipeline. Fields exception,
 * attempts, and outcome are set just before observation.stop() because they are only known at
 * terminal time.
 */
@Getter
@Setter
public class RetryObservationContext extends Observation.Context {

  private String retryName = "";
  private String feature = "unknown";
  private String requestId = "";
  private String exception = "none";
  private int attempts = 0;
  private String outcome = "";

  public RetryObservationContext(String retryName) {
    this.retryName = Optional.ofNullable(retryName).orElse("");
  }
}
