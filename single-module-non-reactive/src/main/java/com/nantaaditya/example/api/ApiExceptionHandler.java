package com.nantaaditya.example.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nantaaditya.example.helper.ContextHelper;
import com.nantaaditya.example.model.constant.ResponseCode;
import com.nantaaditya.example.model.response.Response;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.SQLGrammarException;
import org.postgresql.util.PSQLException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler {

  private final ObjectMapper objectMapper;

  private static final String ERROR_LOG = "#ApiError - exception, ";
  private static final String EXCEPTION_KEY = "exception";

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public Response<Object> methodArgumentNotValid(MethodArgumentNotValidException exception) {
    log.error(ERROR_LOG, exception);

    Response<Object> response = Response.failed(ResponseCode.INVALID_PARAMS, from(exception));
    Map<String, List<String>> errorMaps = from(exception);
    ContextHelper.put(getErrors(errorMaps));

    return response;
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(NoHandlerFoundException.class)
  public Response<Object> noHandlerException(NoHandlerFoundException exception) {
    log.error(ERROR_LOG, exception);

    Map<String, List<String>> errors = Map.of("endpoint", List.of("not available"));
    Response<Object> response = Response.failed(ResponseCode.BAD_REQUEST, errors);
    ContextHelper.put(getErrors(errors));
    return response;
  }

  @ExceptionHandler(SQLGrammarException.class)
  @ResponseBody
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Response<Object> sqlException(SQLGrammarException ex) {
    log.error(ERROR_LOG, ex);
    Response<Object> response = Response.failed(ResponseCode.INTERNAL_ERROR, Collections.emptyMap());
    ContextHelper.put(getErrors(Map.of(EXCEPTION_KEY, List.of(ex.getMessage()))));
    return response;
  }

  @ExceptionHandler(PSQLException.class)
  @ResponseBody
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Response<Object> psqlException(PSQLException ex) {
    log.error(ERROR_LOG, ex);
    Response<Object> response = Response.failed(ResponseCode.INTERNAL_ERROR, Collections.emptyMap());
    ContextHelper.put(getErrors(Map.of(EXCEPTION_KEY, List.of(ex.getMessage()))));
    return response;
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(Throwable.class)
  public Response<Object> throwable(Throwable throwable) {
    log.error(ERROR_LOG, throwable);
    ContextHelper.put(getErrors(Map.of(EXCEPTION_KEY, List.of(throwable.getMessage()))));
    return Response.failed(ResponseCode.INTERNAL_ERROR, Collections.emptyMap());
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public Response<Object> missingServletRequestParameterException(MissingServletRequestParameterException exception) {
    log.error(ERROR_LOG, exception);

    Map<String, List<String>> errors = Map.of(exception.getParameterName(), List.of("missing"));
    Response<Object> response = Response.failed(ResponseCode.BAD_REQUEST, errors);
    ContextHelper.put(getErrors(errors));

    return response;
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(HandlerMethodValidationException.class)
  public Response<Object> handlerMethodValidationException(HandlerMethodValidationException exception) {
    log.error(ERROR_LOG, exception);

    List<String> errorKeys = exception.getParameterValidationResults()
        .stream()
        .map(ParameterValidationResult::getMethodParameter)
        .map(MethodParameter::getParameter)
        .map(Parameter::getName)
        .toList();
    Map<String, List<String>> errors = new HashMap<>();
    for (String errorKey : errorKeys) {
      errors.put(errorKey, List.of("NotValid"));
    }
    Response<Object> response = Response.failed(ResponseCode.INVALID_PARAMS, errors);
    ContextHelper.put(getErrors(errors));

    return response;
  }

  private Map<String, List<String>> from(MethodArgumentNotValidException ex) {
    BindingResult result = ex.getBindingResult();
    if (!result.hasFieldErrors()) {
      return Collections.emptyMap();
    }

    Map<String, List<String>> map = new HashMap<>();

    for (FieldError fieldError : result.getFieldErrors()) {
      String field = fieldError.getField();

      if (!map.containsKey(fieldError.getField())) {
        map.put(field, new LinkedList<>());
      }

      String errorMessage = fieldError.getDefaultMessage();
      map.get(field).add(errorMessage);
    }

    return map;
  }

  private String getErrors(Map<String, List<String>> violations) {
    try {
      return objectMapper.writeValueAsString(violations);
    } catch (JsonProcessingException e) {
      log.error("#ApiError - failed convert errors, ", e);
      return null;
    }
  }
}