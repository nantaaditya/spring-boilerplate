package com.nantaaditya.example.helper;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DateTimeHelperTest {

  @Test
  void getDateInFormat_zonedDateTimeNull() {
    assertNull(DateTimeHelper.getDateInFormat(null, "asdf"));
  }

  @Test
  void getDateInFormat_patternBlank() {
    assertNull(DateTimeHelper.getDateInFormat(ZonedDateTime.now(), ""));
  }

  @Test
  void getDateInFormat_error() {
    assertNull(DateTimeHelper.getDateInFormat(ZonedDateTime.now(), "asdf"));
  }
}