package com.nantaaditya.example.api;

import com.nantaaditya.example.model.request.ExampleRequest;
import com.nantaaditya.example.model.response.ExampleResponse;
import com.nantaaditya.example.model.response.Response;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/example")
public class ExampleController {

  @GetMapping(
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public Response<String> get() {
    return Response.success("Hello World");
  }

  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public Response<ExampleResponse> post(@Valid @RequestBody ExampleRequest request) {
    ExampleResponse exampleResponse = new ExampleResponse(request.name(), request.age());
    return Response.success(exampleResponse);
  }
}
