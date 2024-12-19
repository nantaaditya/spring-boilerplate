package com.nantaaditya.example.api.internal;

import com.nantaaditya.example.model.request.RetryDeadLetterProcessRequest;
import com.nantaaditya.example.model.response.Response;
import com.nantaaditya.example.service.internal.DeadLetterProcessService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/internal-api/dead_letter_process")
@RequiredArgsConstructor
@Tag(name = "internal api", description = "internal api for utility purpose")
public class DeadLetterProcessController {

  private final DeadLetterProcessService deadLetterProcessService;

  @DeleteMapping(
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public Response<Boolean> remove(@RequestParam(required = false, defaultValue = "30") int days) {
    deadLetterProcessService.remove(days);
    return Response.success(true);
  }

  @PostMapping(
      value = "/_retry",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public Response<Boolean> retry(@RequestBody @Valid RetryDeadLetterProcessRequest request) {
    deadLetterProcessService.retry(request);
    return Response.success(true);
  }
}
