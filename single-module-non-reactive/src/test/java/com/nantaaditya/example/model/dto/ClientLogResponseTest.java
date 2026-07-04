package com.nantaaditya.example.model.dto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

@ExtendWith(MockitoExtension.class)
class ClientLogResponseTest {

  @Mock
  private ClientHttpResponse parent;

  @Test
  void constructor_copiesBodyBytes() throws IOException {
    byte[] body = "response-body".getBytes();
    when(parent.getBody()).thenReturn(new ByteArrayInputStream(body));

    ClientLogResponse response = new ClientLogResponse(parent);

    assertNotNull(response.getBodyBytes());
    assertArrayEquals(body, response.getBodyBytes());
  }

  @Test
  void getBody_returnsInputStream() throws IOException {
    byte[] body = "data".getBytes();
    when(parent.getBody()).thenReturn(new ByteArrayInputStream(body));

    ClientLogResponse response = new ClientLogResponse(parent);

    assertNotNull(response.getBody());
  }

  @Test
  void getHeaders_delegatesToParent() throws IOException {
    when(parent.getBody()).thenReturn(new ByteArrayInputStream(new byte[0]));
    when(parent.getHeaders()).thenReturn(new HttpHeaders());

    ClientLogResponse response = new ClientLogResponse(parent);
    response.getHeaders();

    verify(parent).getHeaders();
  }

  @Test
  void getStatusCode_delegatesToParent() throws IOException {
    when(parent.getBody()).thenReturn(new ByteArrayInputStream(new byte[0]));
    when(parent.getStatusCode()).thenReturn(HttpStatus.OK);

    ClientLogResponse response = new ClientLogResponse(parent);
    response.getStatusCode();

    verify(parent).getStatusCode();
  }

  @Test
  void close_delegatesToParent() throws IOException {
    when(parent.getBody()).thenReturn(new ByteArrayInputStream(new byte[0]));

    ClientLogResponse response = new ClientLogResponse(parent);
    response.close();

    verify(parent).close();
  }

  @Test
  void constructor_ioExceptionOnRead_bodyBytesNull() throws IOException {
    when(parent.getBody()).thenThrow(new IOException("stream error"));

    ClientLogResponse response = new ClientLogResponse(parent);

    assertArrayEquals(null, response.getBodyBytes());
  }
}
