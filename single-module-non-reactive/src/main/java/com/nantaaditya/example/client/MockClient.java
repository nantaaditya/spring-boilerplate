package com.nantaaditya.example.client;

import com.nantaaditya.example.helper.RestSender;
import com.nantaaditya.example.helper.RestSenderHelper;
import com.nantaaditya.example.model.response.MockClientResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Component
public class MockClient {

  private RestSender restSender;

  public MockClient(RestSenderHelper restSenderHelper) {
    this.restSender = restSenderHelper.getRestSender("mock");
  }

  public MockClientResponse getMock() {
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    return restSender.execute(
        HttpMethod.GET,
        "todos/1",
        new HttpHeaders(headers),
        null,
        MockClientResponse.class
      )
        .getBody();
  }
}
