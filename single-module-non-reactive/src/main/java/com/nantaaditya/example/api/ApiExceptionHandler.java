package com.nantaaditya.example.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nantaaditya.example.helper.ContextHelper;
import com.nantaaditya.example.model.constant.ResponseCode;
import com.nantaaditya.example.model.response.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler {

  private final ObjectMapper objectMapper;

  private static final String ERROR_LOG = "#ApiError - exception, ";

  @ResponseBody
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Response<Object>> methodArgumentNotValid(MethodArgumentNotValidException exception) {
    log.error(ERROR_LOG, exception);

    Response<Object> response = Response.failed(ResponseCode.INVALID_PARAMS, from(exception));
    ContextHelper.put(collectErrors(exception));

    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  @ResponseBody
  @ExceptionHandler(Throwable.class)
  public ResponseEntity<Response<Object>> throwable(Throwable throwable) {
    log.error(ERROR_LOG, throwable);

    return new ResponseEntity<>(Response.failed(ResponseCode.INTERNAL_ERROR, Map.of()), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private String collectErrors(MethodArgumentNotValidException ex) {
    Map<String, List<String>> errorMaps = from(ex);
    try {
      return objectMapper.writeValueAsString(errorMaps);
    } catch (JsonProcessingException e) {
      log.error("#ApiError - failed convert errors, ", e);
      return null;
    }
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
        map.put(field, new ArrayList<>());
      }

      String errorMessage = fieldError.getDefaultMessage();
      map.get(field).add(errorMessage);
    }

    return map;
  }
}
