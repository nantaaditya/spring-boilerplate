package com.nantaaditya.example.api;

import com.nantaaditya.example.helper.ContextHelper;
import com.nantaaditya.example.helper.ObservationHelper;
import com.nantaaditya.example.helper.ObservationWrapper;
import com.nantaaditya.example.model.constant.ResponseCode;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.model.error.GeneralFlowException;
import com.nantaaditya.example.model.response.Response;
import io.micrometer.observation.Observation;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.hibernate.exception.SQLGrammarException;
import org.postgresql.util.PSQLException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Log4j2
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler {

  private final ObjectMapper objectMapper;
  private final ObservationHelper observationHelper;
  private final ObservationWrapper observationWrapper;
  private final HttpServletRequest request;

  private static final String ERROR_LOG = "#ApiError - exception {}";
  private static final String EXCEPTION_KEY = "exception";
  private static final String ENDPOINT_ERROR_KEY = "endpoint";

  private final Map<String, Integer> errorPriority = new LinkedHashMap<>();

  {
    errorPriority.put("NotBlank", 1);
    errorPriority.put("NotNull", 2);
    errorPriority.put("TooLong", 3);
    errorPriority.put("NotValid", 4);
    errorPriority.put("NotEmpty", 5);
    errorPriority.put("BelowThreshold", 6);
    errorPriority.put("MustPositive", 7);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public Response<Object> methodArgumentNotValid(MethodArgumentNotValidException exception) {
    Map<String, List<String>> errors = from(exception);
    ContextHelper.put(getErrors(errors));
    return generateErrorResponse(exception, errors, ResponseCode.INVALID_PARAMS);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(GeneralFlowException.class)
  public Response<Object> generalFlowException(GeneralFlowException exception) {
    ContextHelper.put(getErrors(exception.getViolations()));
    return generateErrorResponse(exception, exception.getViolations(), exception.getResponse());
  }

  @ResponseBody
  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<Response<Object>> noHandlerException(NoHandlerFoundException exception) {
    Map<String, List<String>> errors = Map.of(ENDPOINT_ERROR_KEY, List.of("not available"));
    ContextHelper.put(getErrors(errors));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(generateErrorResponse(exception, errors, ResponseCode.BAD_REQUEST));
  }

  @ExceptionHandler(SQLGrammarException.class)
  @ResponseBody
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Response<Object> sqlException(SQLGrammarException ex) {
    ContextHelper.put(getErrors(Map.of(EXCEPTION_KEY, List.of(ex.getMessage()))));
    return generateErrorResponse(ex, Collections.emptyMap(), ResponseCode.INTERNAL_ERROR);
  }

  @ExceptionHandler(PSQLException.class)
  @ResponseBody
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Response<Object> psqlException(PSQLException ex) {
    ContextHelper.put(getErrors(Map.of(EXCEPTION_KEY, List.of(ex.getMessage()))));
    return generateErrorResponse(ex, Collections.emptyMap(), ResponseCode.INTERNAL_ERROR);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(Throwable.class)
  public Response<Object> throwable(Throwable throwable) {
    ContextHelper.put(getErrors(Map.of(EXCEPTION_KEY, List.of(throwable.getMessage()))));
    return generateErrorResponse(throwable, Collections.emptyMap(), ResponseCode.INTERNAL_ERROR);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public Response<Object> missingServletRequestParameterException(MissingServletRequestParameterException exception) {
    Map<String, List<String>> errors = Map.of(exception.getParameterName(), List.of("missing"));
    ContextHelper.put(getErrors(errors));
    return generateErrorResponse(exception, errors, ResponseCode.BAD_REQUEST);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(HandlerMethodValidationException.class)
  public Response<Object> handlerMethodValidationException(HandlerMethodValidationException exception) {
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
    ContextHelper.put(getErrors(errors));

    return generateErrorResponse(exception, errors, ResponseCode.INVALID_PARAMS);
  }

  private String getErrors(Map<String, List<String>> violations) {
    try {
      return objectMapper.writeValueAsString(violations);
    } catch (JacksonException e) {
      log.error(AppLogMessage.message("#ApiError - failed convert errors").error(e));
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
      String errorMessage = fieldError.getDefaultMessage();

      map.computeIfAbsent(field, k -> new LinkedList<>()).add(errorMessage);
    }
    map.forEach((key, value) ->
        value.sort(Comparator.comparingInt(msg -> errorPriority.getOrDefault(getMatchingKey(msg), Integer.MAX_VALUE)))
    );

    return map;
  }

  private String getMatchingKey(String errorMessage) {
    return errorPriority.keySet().stream()
        .filter(errorMessage::contains)
        .findFirst()
        .orElse("default");
  }

  private Response<Object> generateErrorResponse(Throwable throwable, Map<String, List<String>> errors,
      ResponseCode responseCode) {
    log.error(AppLogMessage.message(ERROR_LOG, throwable.getMessage()).error(throwable));

    Response<Object> response = Response.failed(responseCode, errors);
    Observation observation = observationWrapper.getObservation(request);
    observationHelper.decorateResponseObservation(observation, throwable, responseCode);
    return response;
  }
}