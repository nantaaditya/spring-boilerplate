package com.nantaaditya.example.helper;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.nantaaditya.example.model.dto.AppLogMessage;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Plugin(
    name = "TextLogLayout",
    category = Node.CATEGORY,
    elementType = Layout.ELEMENT_TYPE,
    printObject = true
)
public class TextLogLayout extends AbstractStringLayout {

  private final ObjectMapper objectMapper = JsonMapper.builder()
      .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(Include.NON_NULL))
      .enable(DeserializationFeature.USE_NULL_FOR_MISSING_REFERENCE_VALUES)
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .build();
  private final String application;

  protected TextLogLayout(String application) {
    super(StandardCharsets.UTF_8);
    this.application = application;
  }

  @PluginFactory
  public static TextLogLayout createLayout(
      @PluginAttribute(value = "application", defaultString = "UNKNOWN") final String application
  ) {
    return new TextLogLayout(application);
  }

  @Override
  public String toSerializable(LogEvent event) {
    StringBuilder sb = new StringBuilder();
    sb.append(DateTimeHelper.getDateInFormat(event.getTimeMillis(), DateTimeHelper.LOG_TIMESTAMP_FORMATTER));
    sb.append(" --- ");
    sb.append(String.format("[%s]", this.application));
    sb.append(" ");
    sb.append(String.format("[%s] ", StringHelper.prependLog(event.getThreadName(), 10, ' ')));
    sb.append(event.getLevel().toString());

    Map<String, String> mdc = event.getContextData().toMap();
    sb.append(String.format(" | requestId: [%s]", mdc.get("requestId")));
    sb.append(String.format(" - trace: [%s-%s]", mdc.get("traceId"), mdc.get("spanId")));
    sb.append(String.format(" | %s : ", StringHelper.prependLog(event.getLoggerName(), 36, ' ')));

    if (event.getMessage() instanceof AppLogMessage a) {
      try {
        sb.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(a));
      } catch (Exception e) {
        sb.append(a.getFormattedMessage());
      }
    } else {
      sb.append(event.getMessage().getFormattedMessage());
    }

    return sb.toString() + System.lineSeparator();
  }

}
