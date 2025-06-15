package com.nantaaditya.example.model.constant;

import lombok.Getter;

@Getter
public enum RetryConstant {

  PROCESS_TYPE("process_type"),
  PROCESS_NAME("process_name");

  private String name;

  RetryConstant(String name) {
    this.name = name;
  }
}
