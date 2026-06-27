package com.nantaaditya.example.model.constant;

import lombok.Getter;

public enum RetryMetricTag {

  RETRY_NAME("retry_name"),
  FEATURE("feature"),
  REQUEST_ID("request_id"),
  EXCEPTION("exception"),
  OUTCOME("outcome"),
  ATTEMPTS("attempts");

  @Getter
  private final String tag;

  RetryMetricTag(String tag) {
    this.tag = tag;
  }
}
