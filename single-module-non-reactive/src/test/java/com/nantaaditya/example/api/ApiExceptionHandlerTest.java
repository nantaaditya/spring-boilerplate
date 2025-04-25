package com.nantaaditya.example.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nantaaditya.example.model.constant.ResponseCode;
import com.nantaaditya.example.model.response.Response;
import java.sql.SQLException;
import org.hibernate.exception.SQLGrammarException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;

@ExtendWith(MockitoExtension.class)
class ApiExceptionHandlerTest {

  @InjectMocks
  private ApiExceptionHandler apiExceptionHandler;

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private MethodValidationResult methodValidationResult;

  @BeforeEach
  void setUp() throws JsonProcessingException {
    when(objectMapper.writeValueAsString(any()))
        .thenReturn("{}");
  }

  @Test
  void noHandlerException() {
    NoHandlerFoundException exception = new NoHandlerFoundException("POST", "/favicon.ico", null);
    Response<Object> response = apiExceptionHandler.noHandlerException(exception);
    assertNotNull(response);
    assertEquals(ResponseCode.BAD_REQUEST.getCode(), response.getResponse().getCode());
  }

  @Test
  void throwable() {
    Throwable throwable = new Throwable("error");
    Response<Object> response = apiExceptionHandler.throwable(throwable);
    assertNotNull(response);
    assertEquals(ResponseCode.INTERNAL_ERROR.getCode(), response.getResponse().getCode());
  }

  @Test
  void sqlException() {
    SQLGrammarException exception = new SQLGrammarException("error", new SQLException());
    assertEquals(ResponseCode.INTERNAL_ERROR.getCode(), apiExceptionHandler.sqlException(exception).getResponse().getCode());
  }

  @Test
  void psqlException() {
    PSQLException exception = new PSQLException("error", PSQLState.UNKNOWN_STATE);
    assertEquals(ResponseCode.INTERNAL_ERROR.getCode(), apiExceptionHandler.psqlException(exception).getResponse().getCode());
  }

  @Test
  void missingServletRequestParameterException() {
    MissingServletRequestParameterException exception = new MissingServletRequestParameterException("key", "String");
    Response<Object> response = apiExceptionHandler.missingServletRequestParameterException(exception);
    assertEquals(ResponseCode.BAD_REQUEST.getCode(), response.getResponse().getCode());
  }

  @Test
  void handlerMethodValidationException() {
    HandlerMethodValidationException exception = new HandlerMethodValidationException(methodValidationResult);
    Response<Object> response = apiExceptionHandler.handlerMethodValidationException(exception);
    assertEquals(ResponseCode.INVALID_PARAMS.getCode(), response.getResponse().getCode());
  }
}