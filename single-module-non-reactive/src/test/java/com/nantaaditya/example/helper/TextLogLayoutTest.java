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
class TextLogLayoutTest {

  private TextLogLayout layout;

  @Mock
  private LogEvent event;

  @Mock
  private ReadOnlyStringMap contextData;

  @BeforeEach
  void setUp() {
    layout = TextLogLayout.createLayout("test-app");
  }

  private void stubEvent() {
    when(event.getTimeMillis()).thenReturn(System.currentTimeMillis());
    when(event.getLevel()).thenReturn(Level.INFO);
    when(event.getLoggerName()).thenReturn("com.example.Service");
    when(event.getThreadName()).thenReturn("main");
    when(event.getContextData()).thenReturn(contextData);
    when(contextData.toMap()).thenReturn(Map.of("traceId", "t1", "spanId", "s1", "requestId", "r1"));
  }

  @Test
  void createLayout_withApplication_isNotNull() {
    assertNotNull(TextLogLayout.createLayout("my-app"));
  }

  @Test
  void toSerializable_plainMessage_containsApplicationAndLevel() {
    stubEvent();
    when(event.getMessage()).thenReturn(new SimpleMessage("hello world"));

    String result = layout.toSerializable(event);

    assertNotNull(result);
    assertTrue(result.contains("[test-app]"));
    assertTrue(result.contains("INFO"));
    assertTrue(result.contains("hello world"));
  }

  @Test
  void toSerializable_containsMdcValues() {
    stubEvent();
    when(event.getMessage()).thenReturn(new SimpleMessage("msg"));

    String result = layout.toSerializable(event);

    assertTrue(result.contains("requestId: [r1]"));
    assertTrue(result.contains("t1-s1"));
  }

  @Test
  void toSerializable_appLogMessage_serializesAsJson() {
    stubEvent();
    AppLogMessage msg = AppLogMessage.message("app log test");
    when(event.getMessage()).thenReturn(msg);

    String result = layout.toSerializable(event);

    assertNotNull(result);
    assertTrue(result.contains("app log test"));
  }

  @Test
  void toSerializable_endsWithLineSeparator() {
    stubEvent();
    when(event.getMessage()).thenReturn(new SimpleMessage("msg"));

    String result = layout.toSerializable(event);

    assertTrue(result.endsWith(System.lineSeparator()));
  }
}
