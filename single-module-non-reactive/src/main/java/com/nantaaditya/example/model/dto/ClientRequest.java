package com.nantaaditya.example.model.dto;

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
    Class<T> responseType,
    RetryContext retryContext,
    String processName
) {

}
