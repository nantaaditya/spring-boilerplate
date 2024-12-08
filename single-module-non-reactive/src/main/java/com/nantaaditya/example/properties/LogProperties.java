package com.nantaaditya.example.properties;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;

@Data
@ConfigurationProperties(value = "apps.log")
public class LogProperties {
  private boolean enableTraceLog;
  private String ignoredTraceLogPath;

  private static final AntPathMatcher matcher = new AntPathMatcher();

  public boolean isIgnoredTraceLogPath(String path) {
    Set<String> ignoredPaths = new HashSet<>();

    if (ignoredTraceLogPath == null || ignoredTraceLogPath.isEmpty())
      return false;

    StringTokenizer tokenizer = new StringTokenizer(ignoredTraceLogPath, ",");
    while (tokenizer.hasMoreTokens()) {
      ignoredPaths.add(tokenizer.nextToken());
    }

    return ignoredPaths.stream()
        .anyMatch(ignoredPath -> matcher.match(ignoredPath, path));
  }
}
