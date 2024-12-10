package com.nantaaditya.example.api.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import com.nantaaditya.example.model.constant.ResponseCode;
import com.nantaaditya.example.model.response.Response;
import com.nantaaditya.example.service.internal.EventLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventLogControllerTest {

  @InjectMocks
  private EventLogController controller;

  @Mock
  private EventLogService eventLogService;

  @Test
  void remove() {
    doNothing().when(eventLogService).remove(30);
    Response<Boolean> result = controller.remove(30);
    assertNotNull(result);
    assertEquals(ResponseCode.SUCCESS.getCode(), result.getResponse().getCode());
    verify(eventLogService).remove(30);
  }
}