package com.nantaaditya.example.helper;

import com.nantaaditya.example.model.constant.FeatureConstant;
import com.nantaaditya.example.model.constant.HeaderConstant;
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
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ObservationHelper {

  private final ObservationRegistry observationRegistry;
  private final TracerHelper tracerHelper;
  private final ContextHelper contextHelper;

  public <S, T> Mono<T> observeApi(ObservationConstant type, S request, Function<S, Mono<T>> processFunction) {
    return execute(
        type.getName(),
        request,
        Set.of(Event.of("type", type.name())),
        processFunction,
        this::createApiContext
    );
  }

  public <S, T, C extends Context, E extends Event> Mono<T> execute(String observationName, S request,
      Set<E> events, Function<S, Mono<T>> processFunction, Supplier<C> contextSupplier) {

    return Mono.deferContextual(ctx -> {
      C context = contextSupplier.get();
      Observation observation = Observation.start(observationName, contextSupplier, observationRegistry);
      Map<String, String> contextMap = MDC.getCopyOfContextMap();
      MDC.setContextMap(contextMap);

      try (Observation.Scope scope = observation.openScope()) {
        Mono<T> result = processFunction.apply(request);

        if (events != null && !events.isEmpty()) {
          events.forEach(observation::event);
        }

        return result
            .doOnError(exception -> doObservationOnError(exception, context, observation))
            .doFinally(signal -> observation.stop());
      }
    });
  }

  public void publishEvent(String key, String value) {
    Observation current = observationRegistry.getCurrentObservation();
    current.event(Event.of(key, value));
  }

  private <C extends Context> void doObservationOnError(Throwable exception, C context,
      Observation observation) {
    log.error("#Observation - error {}", exception.getMessage());
    context.addLowCardinalityKeyValue(KeyValue.of("error", exception.getClass().getName()));
    observation.event(Event.of("error", exception.getClass().getName()));
    observation.error(exception);
  }

  private Context createApiContext() {
    Context observationContext = new Context();
    ContextDTO contextDTO = getAppContext();

    FeatureConstant feature = FeatureConstant.get(contextDTO.getMethod(), contextDTO.getPath());
    if (feature != null) {
      observationContext.addLowCardinalityKeyValue(KeyValue.of("feature", feature.name()));
    }

    if (contextDTO.getRequestId() != null) {
      observationContext.addHighCardinalityKeyValue(KeyValue.of("requestId", contextDTO.getRequestId()));
    }

    return observationContext;
  }

  private ContextDTO getAppContext() {
    String requestId = tracerHelper.getBaggage(HeaderConstant.REQUEST_ID);
    return contextHelper.get(requestId);
  }

}
