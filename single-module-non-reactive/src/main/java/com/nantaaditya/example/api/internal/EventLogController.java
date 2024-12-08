package com.nantaaditya.example.api.internal;

import com.nantaaditya.example.model.response.Response;
import com.nantaaditya.example.service.internal.EventLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/internal-api/event_log")
@RequiredArgsConstructor
public class EventLogController {

  private final EventLogService eventLogService;

  @DeleteMapping(
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public Response<Boolean> remove(@RequestParam(required = false, defaultValue = "30") int days) {
    eventLogService.remove(days);
    return Response.success(true);
  }

}
