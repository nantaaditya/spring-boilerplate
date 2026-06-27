package com.nantaaditya.example.model.constant;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;

public enum RetryFeatureConstant {
  DEFAULT("default");

  @Getter
  private final String name;

  RetryFeatureConstant(String name) {
    this.name = name;
  }

  private static final Map<String, RetryFeatureConstant> BY_NAME = Arrays.stream(values())
      .collect(Collectors.toMap(RetryFeatureConstant::getName, e -> e));

  public static RetryFeatureConstant getByName(String name) {
    return BY_NAME.getOrDefault(name, DEFAULT);
  }
}
