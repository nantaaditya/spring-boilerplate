package com.nantaaditya.example.model.constant;

import lombok.Getter;

@Getter
public enum RetryConstant {

  CLIENT_NAME("client_name"),
  METHOD("method"),
  PATH("path"),
  HEADERS("headers"),
  REQUEST("request"),
  RESPONSE("response"),
  REQUEST_ID("requestId"),
  PROCESS_TYPE("process_type"),
  PROCESS_NAME("process_name"),
  EXCEPTION("exception"),
  MAX_RETRY("max_retry");

  private String name;

  RetryConstant(String name) {
    this.name = name;
  }
}
