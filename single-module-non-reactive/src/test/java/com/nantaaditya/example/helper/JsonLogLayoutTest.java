package com.nantaaditya.example.helper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.nantaaditya.example.model.dto.AppLogMessage;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonLogLayoutTest {

  private JsonLogLayout layout;

  @Mock
  private LogEvent event;

  @Mock
  private ReadOnlyStringMap contextData;

  @BeforeEach
  void setUp() {
    layout = JsonLogLayout.createLayout("1.0.0");
  }

  private void stubEvent() {
    when(event.getTimeMillis()).thenReturn(System.currentTimeMillis());
    when(event.getLevel()).thenReturn(Level.INFO);
    when(event.getLoggerName()).thenReturn("com.example.TestLogger");
    when(event.getContextData()).thenReturn(contextData);
    when(contextData.toMap()).thenReturn(Map.of("traceId", "trace-1", "spanId", "span-1", "requestId", "req-1"));
  }

  @Test
  void createLayout_withVersion_isNotNull() {
    assertNotNull(JsonLogLayout.createLayout("2.0.0"));
  }

  @Test
  void toSerializable_plainMessage_returnsJsonWithFields() {
    stubEvent();
    when(event.getMessage()).thenReturn(new SimpleMessage("test plain message"));

    String result = layout.toSerializable(event);

    assertNotNull(result);
    assertTrue(result.contains("\"level\""));
    assertTrue(result.contains("\"app_version\":\"1.0.0\""));
    assertTrue(result.contains("\"trace_id\":\"trace-1\""));
    assertTrue(result.contains("\"span_id\":\"span-1\""));
    assertTrue(result.contains("\"request_id\":\"req-1\""));
  }

  @Test
  void toSerializable_appLogMessage_returnsJsonWithContext() {
    stubEvent();
    AppLogMessage msg = AppLogMessage.message("operation completed");
    when(event.getMessage()).thenReturn(msg);

    String result = layout.toSerializable(event);

    assertNotNull(result);
    assertTrue(result.contains("\"context\""));
    assertTrue(result.contains("operation completed"));
  }

  @Test
  void toSerializable_appLogMessageWithError_includesErrorContext() {
    stubEvent();
    AppLogMessage msg = AppLogMessage.message("error occurred").error(new RuntimeException("oops"));
    when(event.getMessage()).thenReturn(msg);

    String result = layout.toSerializable(event);

    assertNotNull(result);
    assertTrue(result.contains("\"error\""));
  }

  @Test
  void toSerializable_endsWithLineSeparator() {
    stubEvent();
    when(event.getMessage()).thenReturn(new SimpleMessage("msg"));

    String result = layout.toSerializable(event);

    assertTrue(result.endsWith(System.lineSeparator()));
  }
}
