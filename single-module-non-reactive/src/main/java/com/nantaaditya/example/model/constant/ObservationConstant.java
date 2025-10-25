package com.nantaaditya.example.model.constant;

import lombok.Getter;

@Getter
public enum ObservationConstant {
  INTERNAL_API("api.internal"),
  PUBLIC_API("api.public");

  private String name;

  ObservationConstant(String name) {
    this.name = name;
  }

  public static ObservationConstant from(String path) {
    if (path.startsWith("/internal-api")) {
      return ObservationConstant.INTERNAL_API;
    } else if (path.startsWith("/api")) {
      return ObservationConstant.PUBLIC_API;
    }
    return ObservationConstant.PUBLIC_API;
  }

}
