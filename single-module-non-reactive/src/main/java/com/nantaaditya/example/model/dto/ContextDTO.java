package com.nantaaditya.example.model.dto;

import com.nantaaditya.example.model.response.Response.ResponseMetadata;

public record ContextDTO(
    String clientId,
    String requestId,
    String method,
    String path,
    String requestTime,
    String receivedTime,
    String responseCode,
    String responseDescription,
    String responseTime
) {

  public ContextDTO withDescription(String description) {
    return new ContextDTO(clientId(), requestId(), method(), path(), requestTime(), receivedTime(), responseCode(), description, responseTime());
  }

  public ContextDTO withResponse(ResponseMetadata responseMetadata) {
    return new ContextDTO(clientId(), requestId(), method(), path(), requestTime(), receivedTime(),
        responseMetadata.getCode(), responseMetadata.getDescription(), responseMetadata.getTime());
  }
}
