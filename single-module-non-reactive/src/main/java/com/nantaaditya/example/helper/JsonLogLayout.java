package com.nantaaditya.example.helper;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.nantaaditya.example.model.dto.AppLogMessage;
import io.micrometer.core.instrument.util.StringEscapeUtils;
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
import tools.jackson.databind.node.ObjectNode;

@Plugin(
    name = "JsonLogLayout",
    category = Node.CATEGORY,
    elementType = Layout.ELEMENT_TYPE,
    printObject = true
)
public class JsonLogLayout extends AbstractStringLayout {

  private final ObjectMapper objectMapper = JsonMapper.builder()
      .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(Include.NON_NULL))
      .enable(DeserializationFeature.USE_NULL_FOR_MISSING_REFERENCE_VALUES)
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .build();
  private final String version;

  protected JsonLogLayout(String version) {
    super(StandardCharsets.UTF_8);
    this.version = version;
  }

  @PluginFactory
  public static JsonLogLayout createLayout(
      @PluginAttribute(value = "version", defaultString = "UNKNOWN") final String version
  ) {
    return new JsonLogLayout(version);
  }

  @Override
  public String toSerializable(LogEvent event) {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("timestamp", DateTimeHelper.getDateInFormat(event.getTimeMillis(), DateTimeHelper.LOG_TIMESTAMP_FORMATTER));
    root.put("level", event.getLevel().toString());
    root.put("app_version", this.version);
    root.put("class", StringHelper.prependLog(event.getLoggerName(), 36, ' '));

    ObjectNode context = root.putObject("context");
    if (event.getMessage() instanceof AppLogMessage j) {
      parseJsonLogMessage(j, context);
    } else {
      context.put("message", StringEscapeUtils.escapeJson(event.getMessage().getFormattedMessage()));
    }

    Map<String, String> mdc = event.getContextData().toMap();
    root.put("trace_id", mdc.get("traceId"));
    root.put("span_id", mdc.get("spanId"));
    root.put("request_id", mdc.get("requestId"));

    return root.toString() + System.lineSeparator();
  }

  private void parseJsonLogMessage(AppLogMessage j, ObjectNode context) {
    context.put("message", StringEscapeUtils.escapeJson(j.getFormattedMessage()));

    if (j.getHttpRequest() != null) {
      ObjectNode httpRequest = objectMapper.valueToTree(j.getHttpRequest());
      context.set("http_request", httpRequest);
    }

    if (j.getHttpResponse() != null) {
      ObjectNode httpResponse = objectMapper.valueToTree(j.getHttpResponse());
      context.set("http_response", httpResponse);
    }

    if (j.getError() != null) {
      ObjectNode error = objectMapper.valueToTree(j.getError());
      context.set("error", error);
    }

    if (j.getAdditionalData() != null) {
      try {
        ObjectNode contextData = objectMapper.valueToTree(j.getAdditionalData());
        context.set("additional_data", contextData);
      } catch (Exception e) {
        context.put("additional_data", StringEscapeUtils.escapeJson(j.getAdditionalData().toString()));
      }
    }
  }
}
