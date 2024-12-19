package com.nantaaditya.example.api.internal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import com.nantaaditya.example.model.request.RetryDeadLetterProcessRequest;
import com.nantaaditya.example.model.response.Response;
import com.nantaaditya.example.service.internal.DeadLetterProcessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeadLetterProcessControllerTest {

  @InjectMocks
  private DeadLetterProcessController deadLetterProcessController;

  @Mock
  private DeadLetterProcessService deadLetterProcessService;

  @Test
  void remove() {
    doNothing().when(deadLetterProcessService).remove(30);

    Response<Boolean> result = deadLetterProcessController.remove(30);
    assertNotNull(result);
    assertTrue(result.getData());

    verify(deadLetterProcessService).remove(30);
  }

  @Test
  void retry() {
    RetryDeadLetterProcessRequest request = new RetryDeadLetterProcessRequest(
        "type", "name", 1
    );
    doNothing().when(deadLetterProcessService).retry(request);

    Response<Boolean> result = deadLetterProcessController.retry(request);
    assertNotNull(result);
    assertTrue(result.getData());

    verify(deadLetterProcessService).retry(request);
  }
}