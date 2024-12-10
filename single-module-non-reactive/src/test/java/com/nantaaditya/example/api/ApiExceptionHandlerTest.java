package com.nantaaditya.example.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.nantaaditya.example.model.constant.ResponseCode;
import com.nantaaditya.example.model.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.NoHandlerFoundException;

@ExtendWith(MockitoExtension.class)
class ApiExceptionHandlerTest {

  @InjectMocks
  private ApiExceptionHandler apiExceptionHandler;

  @Test
  void noHandlerException() {
    NoHandlerFoundException exception = new NoHandlerFoundException("POST", "/favicon.ico", null);
    Response<Object> response = apiExceptionHandler.noHandlerException(exception);
    assertNotNull(response);
    assertEquals(ResponseCode.BAD_REQUEST.getCode(), response.getResponse().getCode());
  }

  @Test
  void throwable() {
    Throwable throwable = new Throwable();
    Response<Object> response = apiExceptionHandler.throwable(throwable);
    assertNotNull(response);
    assertEquals(ResponseCode.INTERNAL_ERROR.getCode(), response.getResponse().getCode());
  }
}