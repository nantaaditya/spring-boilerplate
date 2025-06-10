package com.nantaaditya.example.configuration;

import com.google.gson.Gson;
import com.nantaaditya.example.helper.MaskingHelper;
import com.nantaaditya.example.helper.TracerHelper;
import com.nantaaditya.example.helper.TsidHelper;
import com.nantaaditya.example.model.constant.HeaderConstant;
import com.nantaaditya.example.properties.LogProperties;
import io.micrometer.tracing.internal.EncodingUtils;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.ContentType;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.LogbookCreator;
import org.zalando.logbook.core.DefaultHttpLogWriter;
import org.zalando.logbook.core.DefaultSink;
import org.zalando.logbook.core.HeaderFilters;
import org.zalando.logbook.json.JsonHttpLogFormatter;

@Configuration
@RequiredArgsConstructor
public class LogbookWebClientConfiguration {

  private final TracerHelper tracerHelper;
  private final LogProperties logProperties;
  private final Gson gson;

  @Bean
  public Logbook logbook() {
    return LogbookCreator.builder()
        .correlationId(this::composeCorrelationId)
        .headerFilter(HeaderFilters.replaceHeaders(logProperties.getSensitiveFields(), "*"))
        .bodyFilter(jsonBodyFilter())
        .sink(new DefaultSink(new JsonHttpLogFormatter(), new DefaultHttpLogWriter()))
        .build();
  }

  private BodyFilter jsonBodyFilter() {
    return new BodyFilter() {
      @Override
      public String filter(@Nullable String contentType, String body) {
        if (!ContentType.isJsonMediaType(contentType)) {
          return body;
        }

        return MaskingHelper.maskingJson(gson, logProperties.getSensitiveFields(), body);
      }
    };
  }

  private String composeCorrelationId(HttpRequest httpRequest) {
    return Optional.ofNullable(tracerHelper.getBaggage(HeaderConstant.REQUEST_ID))
        .orElseGet(() -> EncodingUtils.fromLong(TsidHelper.generateLongId()));
  }
}
