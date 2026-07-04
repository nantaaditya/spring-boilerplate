package com.nantaaditya.example.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nantaaditya.example.helper.ObservationHelper;
import com.nantaaditya.example.helper.ObservationWrapper;
import com.nantaaditya.example.model.constant.ResponseCode;
import com.nantaaditya.example.model.error.GeneralFlowException;
import com.nantaaditya.example.model.response.Response;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.List;
import org.hibernate.exception.SQLGrammarException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ApiExceptionHandlerTest {

  @InjectMocks
  private ApiExceptionHandler apiExceptionHandler;

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private ObservationHelper observationHelper;

  @Mock
  private ObservationWrapper observationWrapper;

  @Mock
  private HttpServletRequest request;

  @Mock
  private MethodValidationResult methodValidationResult;

  @Mock
  private BindingResult bindingResult;

  private MethodParameter methodParameter;

  @BeforeEach
  void setUp() throws JacksonException, NoSuchMethodException {
    when(objectMapper.writeValueAsString(any())).thenReturn("{}");
    methodParameter = new MethodParameter(
        ApiExceptionHandlerTest.class.getDeclaredMethod("dummyTarget", String.class), 0);
  }

  private void dummyTarget(String field) {
    // reflection target for building a real MethodParameter in tests
  }

  @Test
  void noHandlerException() {
    NoHandlerFoundException exception = new NoHandlerFoundException("POST", "/favicon.ico", null);
    ResponseEntity<Response<Object>> response = apiExceptionHandler.noHandlerException(exception);
    assertNotNull(response);
    assertEquals(ResponseCode.BAD_REQUEST.getCode(), response.getBody().getResponse().getCode());
  }

  @Test
  void generalFlowException() {
    GeneralFlowException exception = new GeneralFlowException(ResponseCode.BAD_REQUEST);
    Response<Object> response = apiExceptionHandler.generalFlowException(exception);
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

  @Test
  void methodArgumentNotValid_sortsErrorsByPriority() {
    FieldError notEmptyError = new FieldError("obj", "field", "must not be NotEmpty");
    FieldError notBlankError = new FieldError("obj", "field", "must not be NotBlank");
    when(bindingResult.hasFieldErrors()).thenReturn(true);
    when(bindingResult.getFieldErrors()).thenReturn(List.of(notEmptyError, notBlankError));

    MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);
    Response<Object> response = apiExceptionHandler.methodArgumentNotValid(exception);

    assertEquals(ResponseCode.INVALID_PARAMS.getCode(), response.getResponse().getCode());
    assertEquals(List.of("must not be NotBlank", "must not be NotEmpty"),
        response.getError().getViolations().get("field"));
  }

  @Test
  void methodArgumentNotValid_noFieldErrors_emptyViolations() {
    when(bindingResult.hasFieldErrors()).thenReturn(false);

    MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);
    Response<Object> response = apiExceptionHandler.methodArgumentNotValid(exception);

    assertEquals(ResponseCode.INVALID_PARAMS.getCode(), response.getResponse().getCode());
    assertTrue(response.getError().getViolations().isEmpty());
  }
}