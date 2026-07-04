package com.nantaaditya.example.entity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nantaaditya.example.model.constant.RetryConstant;
import com.nantaaditya.example.model.constant.RetryStatus;
import com.nantaaditya.example.model.dto.DeadLetterCapable;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeadLetterProcessTest {

  @Test
  void create_fromRetryContext_setsAllFields() {
    Map<String, Object> ctx = Map.of(
        RetryConstant.PROCESS_TYPE.getName(), "HTTP",
        RetryConstant.PROCESS_NAME.getName(), "example-service",
        RetryConstant.REQUEST_ID.getName(), "req-123",
        RetryConstant.CLIENT_NAME.getName(), "client-a",
        RetryConstant.METHOD.getName(), "POST",
        RetryConstant.PATH.getName(), "/api/test",
        RetryConstant.HEADERS.getName(), "{}",
        RetryConstant.MAX_RETRY.getName(), 3
    );
    byte[] payload = "payload".getBytes();
    byte[] histories = "hist".getBytes();
    Throwable ex = new RuntimeException("failure");

    DeadLetterProcess dlp = DeadLetterProcess.create(ctx, payload, histories, ex);

    assertEquals("HTTP", dlp.getProcessType());
    assertEquals("example-service", dlp.getProcessName());
    assertEquals("req-123", dlp.getIdempotencyKey());
    assertEquals("client-a", dlp.getClientName());
    assertEquals("POST", dlp.getMethod());
    assertEquals("/api/test", dlp.getPath());
    assertEquals("{}", dlp.getHeaders());
    assertArrayEquals(payload, dlp.getPayload());
    assertEquals(0, dlp.getRetryCount());
    assertEquals(3, dlp.getMaxRetry());
    assertEquals(RetryStatus.NEW.name(), dlp.getStatus());
    assertEquals("failure", dlp.getLastError());
    assertArrayEquals(histories, dlp.getRetryHistories());
  }

  @Test
  void create_fromRetryContext_maxRetryNotInteger_defaultsToZero() {
    Map<String, Object> ctx = Map.of(
        RetryConstant.PROCESS_TYPE.getName(), "HTTP",
        RetryConstant.PROCESS_NAME.getName(), "svc",
        RetryConstant.REQUEST_ID.getName(), "id",
        RetryConstant.CLIENT_NAME.getName(), "c",
        RetryConstant.METHOD.getName(), "GET",
        RetryConstant.PATH.getName(), "/",
        RetryConstant.HEADERS.getName(), "",
        RetryConstant.MAX_RETRY.getName(), "not-an-int"
    );

    DeadLetterProcess dlp = DeadLetterProcess.create(ctx, null, null, new RuntimeException("err"));

    assertEquals(0, dlp.getMaxRetry());
  }

  @Test
  void create_fromDeadLetterCapable_setsAllFields() {
    DeadLetterCapable capable = mock(DeadLetterCapable.class);
    when(capable.getProcessType()).thenReturn("TASK");
    when(capable.getProcessName()).thenReturn("my-task");
    when(capable.getPayload()).thenReturn("data".getBytes());

    DeadLetterProcess dlp = DeadLetterProcess.create(capable);

    assertEquals("TASK", dlp.getProcessType());
    assertEquals("my-task", dlp.getProcessName());
    assertArrayEquals("data".getBytes(), dlp.getPayload());
    assertEquals(0, dlp.getRetryCount());
    assertEquals(1, dlp.getMaxRetry());
    assertEquals(RetryStatus.NEW.name(), dlp.getStatus());
    assertEquals("Task rejected: async executor queue full", dlp.getLastError());
  }

  @Test
  void create_fromRunnable_setsProcessTypeAndName() {
    Runnable task = () -> {};

    DeadLetterProcess dlp = DeadLetterProcess.create(task, "ASYNC");

    assertEquals("ASYNC", dlp.getProcessType());
    assertNotNull(dlp.getProcessName());
    assertEquals(0, dlp.getRetryCount());
    assertEquals(1, dlp.getMaxRetry());
    assertEquals(RetryStatus.NEW.name(), dlp.getStatus());
    assertEquals("Task rejected: async executor queue full", dlp.getLastError());
  }

  @Test
  void markAsSuccess_setsStatusToSuccess() {
    DeadLetterProcess dlp = DeadLetterProcess.builder()
        .processType("HTTP")
        .processName("svc")
        .retryCount(2)
        .maxRetry(3)
        .status(RetryStatus.RETRYING.name())
        .lastError("some error")
        .build();

    dlp.markAsSuccess();

    assertEquals(RetryStatus.SUCCESS.name(), dlp.getStatus());
    assertNotNull(dlp.getUpdatedDate());
  }

  @Test
  void markRetry_incrementsRetryCount() {
    DeadLetterProcess dlp = DeadLetterProcess.builder()
        .processType("HTTP")
        .processName("svc")
        .retryCount(1)
        .maxRetry(3)
        .status(RetryStatus.RETRYING.name())
        .lastError("error")
        .build();

    dlp.markRetry();

    assertEquals(2, dlp.getRetryCount());
    assertNotNull(dlp.getUpdatedDate());
  }
}
