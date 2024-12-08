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
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler {

  private final ObjectMapper objectMapper;

  private static final String ERROR_LOG = "#ApiError - exception, ";

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public Response<Object> methodArgumentNotValid(MethodArgumentNotValidException exception) {
    log.error(ERROR_LOG, exception);

    Response<Object> response = Response.failed(ResponseCode.INVALID_PARAMS, from(exception));
    ContextHelper.put(collectErrors(exception));

    return response;
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(Throwable.class)
  public Response<Object> throwable(Throwable throwable) {
    log.error(ERROR_LOG, throwable);

    return Response.failed(ResponseCode.INTERNAL_ERROR, Map.of());
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
