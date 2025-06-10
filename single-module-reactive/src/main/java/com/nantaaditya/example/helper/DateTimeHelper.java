package com.nantaaditya.example.helper;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DateTimeHelper {

  public static final String ISO_8601_GMT7_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
  public static final ZoneId ZONE_ID = ZoneId.systemDefault();

  private DateTimeHelper() {}

  public static String getDateInFormat(ZonedDateTime zonedDateTime, String pattern) {
    if (zonedDateTime == null || pattern.isBlank()) return null;

    try {
      return zonedDateTime.format(DateTimeFormatter.ofPattern(pattern));
    } catch (Exception e) {
      log.error("#DateTime - failed convert {}, pattern {} cause {}",
          zonedDateTime, pattern, e.getMessage(), ErrorHelper.getRootCause(e));
      return null;
    }
  }
}
