package com.nantaaditya.example.api;

import com.nantaaditya.example.client.MockClient;
import com.nantaaditya.example.helper.ObservationHelper;
import com.nantaaditya.example.model.request.ExampleRequest;
import com.nantaaditya.example.model.response.ExampleResponse;
import com.nantaaditya.example.model.response.MockClientResponse;
import com.nantaaditya.example.model.response.Response;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/example")
public class ExampleController {

  private final ObservationHelper observationHelper;
  private final MockClient mockClient;

  @GetMapping(
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public Response<String> get() {
    return observationHelper.observeApi(
        "Hello world", Response::success);
  }

  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public Response<ExampleResponse> post(@Valid @RequestBody ExampleRequest request) {
    return observationHelper.observeApi(request, r -> {
      ExampleResponse exampleResponse = new ExampleResponse(request.name(), request.age());
      return Response.success(exampleResponse);
    });
  }

  @GetMapping(
      value = "/mock",
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public Response<MockClientResponse> getMock() {
    return Response.success(mockClient.getMock());
  }
}
