package com.nantaaditya.example.helper;

import com.nantaaditya.example.model.dto.AppLogMessage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class DateTimeHelper {

  public static final String ISO_8601_GMT7_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
  public static final String LOG_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss:SSS";
  public static final ZoneId ZONE_ID = ZoneId.systemDefault();

  public static final DateTimeFormatter ISO_8601_FORMATTER = DateTimeFormatter.ofPattern(ISO_8601_GMT7_FORMAT);
  public static final DateTimeFormatter LOG_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(LOG_TIMESTAMP_FORMAT);

  private DateTimeHelper() {}

  public static String getDateInFormat(ZonedDateTime zonedDateTime, String pattern) {
    if (zonedDateTime == null || pattern.isBlank()) return null;

    try {
      return zonedDateTime.format(DateTimeFormatter.ofPattern(pattern));
    } catch (Exception e) {
      log.error(AppLogMessage.message("#DateTime - failed convert {}, pattern {}", zonedDateTime, pattern).error(e));
      return null;
    }
  }

  public static String getDateInFormat(long timemillis, DateTimeFormatter dateTimeFormatter) {
    if (timemillis <= 0 || dateTimeFormatter == null) return null;
    try {
      LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timemillis), ZONE_ID);
      return localDateTime.format(dateTimeFormatter);
    } catch (Exception e) {
      log.error(AppLogMessage.message("#DateTime - failed convert {}, pattern {}", timemillis, dateTimeFormatter).error(e));
      return null;
    }
  }
}
