package com.nantaaditya.example.model.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

class ClientRequestTest {

  private final ParameterizedTypeReference<String> responseType = new ParameterizedTypeReference<>() {};

  @Test
  void getFullPath_noQueryParams_returnsPathAsIs() {
    ClientRequest<Void, String> req = new ClientRequest<>(
        HttpMethod.GET, "/api/users", null, new HttpHeaders(), null, responseType, null, "op");

    assertEquals("/api/users", req.getFullPath());
  }

  @Test
  void getFullPath_singleQueryParam_appendsToPath() {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.put("id", List.of("42"));
    ClientRequest<Void, String> req = new ClientRequest<>(
        HttpMethod.GET, "/api/users", params, new HttpHeaders(), null, responseType, null, "op");

    assertEquals("/api/users?id=42", req.getFullPath());
  }

  @Test
  void getFullPath_multipleQueryParams_appendsAllToPath() {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.put("page", List.of("1"));
    params.put("size", List.of("10"));
    ClientRequest<Void, String> req = new ClientRequest<>(
        HttpMethod.GET, "/api/items", params, new HttpHeaders(), null, responseType, null, "op");

    String fullPath = req.getFullPath();
    assertTrue(fullPath.startsWith("/api/items?"));
    assertTrue(fullPath.contains("page=1"));
    assertTrue(fullPath.contains("size=10"));
  }
}
