package com.nantaaditya.example.model.constant;

import lombok.Getter;

@Getter
public enum ObservationConstant {
  PUBLIC_API("api.public");

  private String name;

  ObservationConstant(String name) {
    this.name = name;
  }
}
