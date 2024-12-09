package com.nantaaditya.example.properties;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.AntPathMatcher;

@Slf4j
@Data
@ConfigurationProperties(value = "apps.log")
public class LogProperties {
  private boolean enableTraceLog;
  private String ignoredTraceLogPath;
  private String sensitiveField;

  private static final AntPathMatcher matcher = new AntPathMatcher();

  public boolean isSensitiveField(String key) {
    if (sensitiveField == null || sensitiveField.isEmpty()) {
      return false;
    }

    return toCollections(sensitiveField)
        .stream()
        .anyMatch(field -> field.equals(key));
  }

  public boolean isIgnoredTraceLogPath(String path) {
    if (ignoredTraceLogPath == null || ignoredTraceLogPath.isEmpty())
      return false;

    return toCollections(ignoredTraceLogPath)
        .stream()
        .anyMatch(ignoredPath -> matcher.match(ignoredPath, path));
  }

  public Set<String> toCollections(String fields) {
    Set<String> collections = new HashSet<>();

    if (fields == null || fields.isEmpty()) return collections;

    StringTokenizer tokenizer = new StringTokenizer(fields, ",");
    while (tokenizer.hasMoreTokens()) {
      collections.add(tokenizer.nextToken());
    }
    log.debug("#Converter - String field to collections {}", collections);
    return collections;
  }
}
