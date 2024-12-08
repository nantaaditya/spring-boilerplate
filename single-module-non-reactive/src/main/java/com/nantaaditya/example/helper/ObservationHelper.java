package com.nantaaditya.example.helper;

import com.nantaaditya.example.model.constant.FeatureConstant;
import com.nantaaditya.example.model.constant.ObservationConstant;
import com.nantaaditya.example.model.dto.ContextDTO;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.Observation.Event;
import io.micrometer.observation.ObservationRegistry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ObservationHelper {

  private final ObservationRegistry observationRegistry;

  public <S, T, C extends Context, E extends Event> T observeApi(S request,
      Function<S, T> processFunction) {
    return observe(
        ObservationConstant.PUBLIC_API.getName(),
        request,
        null,
        processFunction,
        ObservationHelper::createApiContext
    );
  }

  public <S, T, C extends Context, E extends Event> T observe(String observeName,
      S request, Set<E> events, Function<S, T> processFunction, Supplier<C> contextSupplier) {

    C context = contextSupplier.get();
    Observation observation = Observation.start(observeName, contextSupplier, observationRegistry);

    try (Observation.Scope scope = observation.openScope()) {
      T result = processFunction.apply(request);

      if (events != null && !events.isEmpty()) {
        events.forEach(observation::event);
      }

      return result;
    } catch (Exception ex) {
      log.error("#Observation - observe error {}", observeName, ex.getMessage());

      context.addLowCardinalityKeyValue(KeyValue.of("error", ex.getCause().getClass().getName()));
      observation.event(Event.of("error", ex.getCause().getClass().getName()));
      observation.error(ex);

      throw ex;
    } finally {
      observation.stop();
    }
  }

  private static Context createApiContext() {
    Context observationContext = new Context();
    ContextDTO contextDTO = ContextHelper.get();

    FeatureConstant featureConstant = FeatureConstant.get(contextDTO.method(), contextDTO.path());

    if (featureConstant != null) {
      observationContext.addHighCardinalityKeyValue(KeyValue.of("feature", featureConstant.name()));
    }

    if (contextDTO.requestId() != null) {
      observationContext.addLowCardinalityKeyValue(KeyValue.of("requestId", contextDTO.requestId()));
    }

    return observationContext;
  }

}
