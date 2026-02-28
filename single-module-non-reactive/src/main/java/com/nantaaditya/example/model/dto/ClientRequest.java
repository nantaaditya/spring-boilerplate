package com.nantaaditya.example.model.dto;

import java.beans.Transient;
import java.util.List;
import java.util.Map.Entry;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.retry.RetryContext;
import org.springframework.util.MultiValueMap;

public record ClientRequest<S, T>(
    HttpMethod method,
    String path,
    MultiValueMap<String, String> queryParams,
    HttpHeaders headers,
    S request,
    ParameterizedTypeReference<T> responseType,
    RetryContext retryContext,
    String processName
) {

  @Transient
  public String getFullPath() {
    StringBuilder pathBuilder = new StringBuilder(this.path());
    if (this.queryParams() != null) {
      pathBuilder.append("?");

      MultiValueMap<String, String> queryParams = this.queryParams();
      for (Entry<String, List<String>> entry : queryParams.entrySet()) {
        pathBuilder.append(entry.getKey()).append("=").append(entry.getValue().getFirst()).append("&");
      }

      pathBuilder.deleteCharAt(pathBuilder.length() - 1);
    }
    return pathBuilder.toString();
  }
}
