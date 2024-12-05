package com.nantaaditya.example.model.request;

import lombok.Data;

@Data
public class ExampleTestRequest {
  private String name;
  private int age;

  private static ExampleTestRequest instance;

  private ExampleTestRequest() { }

  public static ExampleTestRequest getInstance() {
    synchronized (ExampleTestRequest.class) {
      if (instance == null) {
        instance = new ExampleTestRequest();
      }
    }
    return instance;
  }
}
