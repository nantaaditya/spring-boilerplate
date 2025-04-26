package com.nantaaditya.example.helper;

import com.nantaaditya.example.model.constant.FeatureConstant;
import com.nantaaditya.example.model.constant.ObservationConstant;
import com.nantaaditya.example.model.dto.ContextDTO;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.Observation.Event;
import io.micrometer.observation.ObservationRegistry;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ObservationHelper {

  private final ObservationRegistry observationRegistry;

  public <S, T> T observeApi(S request, Function<S, T> processFunction) {
    return execute(
        ObservationConstant.PUBLIC_API.getName(),
        request,
        null,
        processFunction,
        ObservationHelper::createApiContext
    );
  }

  public <S, T, C extends Context, E extends Event> T execute(String observationName, S request, Set<E> events,
      Function<S, T> processFunction, Supplier<C> contextSupplier) {

    C context = contextSupplier.get();
    Observation observation = Observation.start(observationName, contextSupplier, observationRegistry);
    Map<String, String> contextMap = MDC.getCopyOfContextMap();

    try (Observation.Scope scope = observation.openScope()) {
      MDC.setContextMap(contextMap);
      T result = processFunction.apply(request);

      if (events != null && !events.isEmpty()) {
        events.forEach(observation::event);
      }

      return result;
    } catch (Exception ex) {
      log.error("#Observation - error {}", ex.getMessage());
      context.addLowCardinalityKeyValue(KeyValue.of("error", ex.getClass().getName()));
      observation.event(Event.of("error", ex.getClass().getName()));
      observation.error(ex);
      throw ex;
    } finally {
      observation.stop();
    }
  }

  private static Context createApiContext() {
    Context observationContext = new Context();
    ContextDTO contextDTO = ContextHelper.get();

    FeatureConstant feature = FeatureConstant.get(contextDTO.method(), contextDTO.path());
    if (feature != null) {
      observationContext.addLowCardinalityKeyValue(KeyValue.of("feature", feature.name()));
    }

    if (contextDTO.requestId() != null) {
      observationContext.addHighCardinalityKeyValue(KeyValue.of("requestId", contextDTO.requestId()));
    }

    return observationContext;
  }

}
