package com.nantaaditya.example.model.constant;

import lombok.Getter;

public enum ResponseCode {
  SUCCESS("000", "success"),
  INVALID_PARAMS("900", "invalid parameters"),
  INTERNAL_ERROR("901", "internal error"),;

  @Getter
  private String code;
  @Getter
  private String message;

  ResponseCode(String code, String message) {
    this.code = code;
    this.message = message;
  }
}
