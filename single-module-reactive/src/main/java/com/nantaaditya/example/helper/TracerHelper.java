package com.nantaaditya.example.helper;

import com.nantaaditya.example.model.constant.HeaderConstant;
import io.micrometer.tracing.Tracer;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TracerHelper {

  private final Tracer tracer;

  public Map<String, String> getBaggages() {
    return tracer.getAllBaggage();
  }

  public String getBaggage(HeaderConstant header) {
    return getBaggages().getOrDefault(header.getHeader(), null);
  }
}
