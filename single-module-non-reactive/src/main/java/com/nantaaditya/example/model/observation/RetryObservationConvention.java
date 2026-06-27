package com.nantaaditya.example.model.observation;

import com.nantaaditya.example.model.constant.RetryMetricTag;
import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationConvention;
import java.util.Optional;

/**
 * Maps a RetryObservationContext to the metric name and tag set. Five LOW cardinality tags
 * (retry_name, feature, exception, outcome, attempts) are emitted as meter tags.
 * request_id is HIGH cardinality and is stored in span context only.
 */
public class RetryObservationConvention implements ObservationConvention<RetryObservationContext> {

  public static final String OBSERVATION_NAME = "app.retry";

  @Override
  public boolean supportsContext(Context context) {
    return context instanceof RetryObservationContext;
  }

  @Override
  public KeyValues getLowCardinalityKeyValues(RetryObservationContext context) {
    return KeyValues.of(
        KeyValue.of(RetryMetricTag.RETRY_NAME.getTag(), safe(context.getRetryName())),
        KeyValue.of(RetryMetricTag.FEATURE.getTag(), safe(context.getFeature())),
        KeyValue.of(RetryMetricTag.EXCEPTION.getTag(), safe(context.getException())),
        KeyValue.of(RetryMetricTag.OUTCOME.getTag(), safe(context.getOutcome())),
        KeyValue.of(RetryMetricTag.ATTEMPTS.getTag(), String.valueOf(context.getAttempts()))
    );
  }

  @Override
  public KeyValues getHighCardinalityKeyValues(RetryObservationContext context) {
    return KeyValues.of(
        KeyValue.of(RetryMetricTag.REQUEST_ID.getTag(), safe(context.getRequestId()))
    );
  }

  @Override
  public String getName() {
    return OBSERVATION_NAME;
  }

  private String safe(String value) {
    return Optional.ofNullable(value).orElse("");
  }
}
