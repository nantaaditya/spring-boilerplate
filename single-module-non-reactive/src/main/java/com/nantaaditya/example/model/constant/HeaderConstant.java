package com.nantaaditya.example.model.constant;

import java.util.stream.Stream;
import lombok.Getter;

public enum HeaderConstant {
  CLIENT_ID("x-client-id"),
  REQUEST_ID("x-request-id"),
  REQUEST_TIME("x-request-time"),
  RECEIVED_TIME("x-received-time"),
  RESPONSE_TIME("x-response-time");

  @Getter
  private String header;

  HeaderConstant(String header) {
    this.header = header;
  }

  public static boolean contains(String header) {
    return Stream.of(values())
        .anyMatch(v -> v.getHeader().equals(header));
  }
}
