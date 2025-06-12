package com.nantaaditya.example.helper;

import com.nantaaditya.example.model.constant.HeaderConstant;
 import io.micrometer.tracing.Baggage;
import io.micrometer.tracing.BaggageManager;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TracerHelper {

  private final Tracer tracer;
  private final BaggageManager baggageManager;

  public Map<String, String> getBaggages() {
    return tracer.getAllBaggage();
  }

  public String getBaggage(HeaderConstant header) {
    return getBaggages().getOrDefault(header.getHeader(), null);
  }

  public void setBaggage(String key, String value) {
    Span currentSpan = tracer.currentSpan();

    try {
      if (currentSpan != null) {
        Baggage baggage = Optional.ofNullable(baggageManager.getBaggage(key))
            .orElseGet(() -> baggageManager.createBaggage(key));
        baggage.makeCurrent(value);
        currentSpan.tag(key, value);
      }
    } catch (Exception e) {
      log.error("#Baggage - failed to set baggage {} with value {}, error {} cause {}", key, value,
          e.getMessage(), ErrorHelper.getRootCause(e));
    }
  }
}
