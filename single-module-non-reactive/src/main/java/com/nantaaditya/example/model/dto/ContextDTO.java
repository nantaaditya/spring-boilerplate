package com.nantaaditya.example.model.dto;

import com.nantaaditya.example.model.constant.HeaderConstant;
import com.nantaaditya.example.model.response.Response.ResponseMetadata;
import java.beans.Transient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;

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

  public static ContextDTO from(HttpRequest httpRequest) {
    HttpHeaders httpHeaders = httpRequest.getHeaders();
    return new ContextDTO(
        httpHeaders.getFirst(HeaderConstant.CLIENT_ID.getHeader()),
        httpHeaders.getFirst(HeaderConstant.REQUEST_ID.getHeader()),
        httpRequest.getMethod().name(),
        httpRequest.getURI().getPath(),
        httpHeaders.getFirst(HeaderConstant.REQUEST_TIME.getHeader()),
        null,
        null,
        null,
        null
    );
  }

  @Transient
  public String getUnknownFeature() {
    return method + "_" + path;
  }
}
