package com.nantaaditya.example.model.request;

import lombok.Data;

@Data
public class HeaderRequest {
  private String clientId;
  private String requestId;
  private String requestTime;

  private static HeaderRequest instance;

  public static HeaderRequest getInstance() {
    synchronized (HeaderRequest.class) {
      if (instance == null) {
        instance = new HeaderRequest();
      }
    }
    return instance;
  }

  private HeaderRequest() {

  }
}
