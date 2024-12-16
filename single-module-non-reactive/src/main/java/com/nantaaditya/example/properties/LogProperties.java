package com.nantaaditya.example.properties;

import com.nantaaditya.example.helper.StringHelper;
import java.util.HashSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.AntPathMatcher;

@Slf4j
@ConfigurationProperties(value = "apps.log")
public record LogProperties(
    boolean enableTraceLog,
    String ignoredTraceLogPath,
    String sensitiveField
) {

  private static final AntPathMatcher matcher = new AntPathMatcher();

  public boolean isSensitiveField(String key) {
    if (sensitiveField == null || sensitiveField.isEmpty()) {
      return false;
    }

    return StringHelper.toCollection(sensitiveField, ",", HashSet.class)
        .stream()
        .anyMatch(field -> field.equals(key));
  }

  public boolean isIgnoredTraceLogPath(String path) {
    if (ignoredTraceLogPath == null || ignoredTraceLogPath.isEmpty())
      return false;

    return StringHelper.toCollection(ignoredTraceLogPath, ",", HashSet.class)
        .stream()
        .anyMatch(ignoredPath -> matcher.match(ignoredPath, path));
  }
}
