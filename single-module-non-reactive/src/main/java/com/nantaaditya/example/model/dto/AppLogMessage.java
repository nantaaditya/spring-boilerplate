package com.nantaaditya.example.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.message.Message;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppLogMessage implements Message {
  private String message;
  @JsonProperty("http_request")
  private JsonLogHttpRequest httpRequest;
  @JsonProperty("http_response")
  private JsonLogHttpResponse httpResponse;
  private JsonLogError error;
  @JsonProperty("additional_data")
  private Object additionalData;

  public static AppLogMessage create(String message) {
    return new AppLogMessage(message, null, null, null, Map.of());
  }

  public static AppLogMessage create(String message, Object contextData) {
    return new AppLogMessage(message, null, null, null, contextData);
  }

  public static AppLogMessage create(String message, JsonLogHttpRequest httpRequest) {
    return new AppLogMessage(message, httpRequest, null, null, Map.of());
  }

  public static AppLogMessage create(String message, JsonLogHttpResponse httpResponse) {
    return new AppLogMessage(message, null, httpResponse, null, Map.of());
  }

  public static AppLogMessage create(String message, Throwable throwable) {
    return new AppLogMessage(message, null, null, JsonLogError.create(throwable), Map.of());
  }

  public static AppLogMessage create(String message, Throwable throwable, Object contextData) {
    return new AppLogMessage(message, null, null, JsonLogError.create(throwable), contextData);
  }

  @Override
  @JsonIgnore
  public String getFormattedMessage() {
    return message;
  }

  @Override
  public Object[] getParameters() {
    return null;
  }

  @Override
  public Throwable getThrowable() {
    return null;
  }
}
