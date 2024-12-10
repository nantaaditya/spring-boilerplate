package com.nantaaditya.example.listener;

import com.nantaaditya.example.model.constant.ObservationConstant;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.Observation.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppObservationListenerTest {

  @InjectMocks
  private AppObservationListener listener;

  @Test
  void onEvent_skip() {
    Context context = new Context();
    context.setName("other");

    listener.onEvent(null, context);
  }

  @Test
  void onEvent() {
    Context context = new Context();
    context.setName(ObservationConstant.PUBLIC_API.getName());

    Event event = Event.of("key", "value");
    listener.onEvent(event, context);
  }

  @Test
  void onError_skip() {
    Context context = new Context();
    context.setName("other");

    listener.onError(context);
  }

  @Test
  void onError() {
    Context context = new Context();
    context.setName(ObservationConstant.PUBLIC_API.getName());

    listener.onError(context);
  }
}