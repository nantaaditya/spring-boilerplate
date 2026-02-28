package com.nantaaditya.example.api;

import com.nantaaditya.example.client.MockClient;
import com.nantaaditya.example.model.request.ExampleRequest;
import com.nantaaditya.example.model.response.ExampleResponse;
import com.nantaaditya.example.model.response.MockClientResponse;
import com.nantaaditya.example.model.response.Response;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/example")
public class ExampleController extends BaseController {

  private final MockClient mockClient;

  @GetMapping(
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public ResponseEntity<Response<String>> get() {
    return toResponse(Response.success("Hello world"));
  }

  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public ResponseEntity<Response<ExampleResponse>> post(@Valid @RequestBody ExampleRequest request) {
    ExampleResponse exampleResponse = new ExampleResponse(request.name(), request.age());
    return toResponse(Response.success(exampleResponse));
  }

  @GetMapping(
      value = "/mock",
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public ResponseEntity<Response<MockClientResponse>> getMock() {
    return toResponse(Response.success(mockClient.getMock()));
  }
}
