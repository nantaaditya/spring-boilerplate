package com.nantaaditya.example.api;

import com.nantaaditya.example.helper.ContextHelper;
import com.nantaaditya.example.helper.ObservationHelper;
import com.nantaaditya.example.helper.ObservationWrapper;
import com.nantaaditya.example.model.constant.ResponseCode;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.model.response.Response;
import com.nantaaditya.example.model.response.Response.ErrorMetadata;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Log4j2
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler {

  private final ObjectMapper objectMapper;
  private final ObservationHelper observationHelper;
  private final ObservationWrapper observationWrapper;

  private static final String ERROR_LOG = "#ApiError - got error exception";
  private static final String EXCEPTION_KEY = "exception";

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public Response<Object> methodArgumentNotValid(MethodArgumentNotValidException exception) {
    return toError(exception, error -> {
      Map<String, List<String>> errors = from(error);
      Response<Object> response = Response.failed(ResponseCode.INVALID_PARAMS, errors);
      return Pair.of(errors, response);
    });
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(NoHandlerFoundException.class)
  public Response<Object> noHandlerException(NoHandlerFoundException exception) {
    return toError(exception, error -> {
      Map<String, List<String>> errors = Map.of("endpoint", List.of("not available"));
      Response<Object> response = Response.failed(ResponseCode.BAD_REQUEST, errors);
      return Pair.of(errors, response);
    });
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(NoSuchElementException.class)
  public Response<Object> noSuchElementException(NoSuchElementException exception) {
    return toError(exception, error -> {
      Map<String, List<String>> errors = Map.of("id", List.of("NotFound"));
      Response<Object> response = Response.failed(ResponseCode.NOT_FOUND, errors);
      return Pair.of(errors, response);
    });
  }

  @ExceptionHandler(SQLGrammarException.class)
  @ResponseBody
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Response<Object> sqlException(SQLGrammarException exception) {
    return toBaseError(exception);
  }

  @ExceptionHandler(PSQLException.class)
  @ResponseBody
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Response<Object> psqlException(PSQLException exception) {
    return toBaseError(exception);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(Throwable.class)
  public Response<Object> throwable(Throwable throwable) {
    return toBaseError(throwable);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public Response<Object> missingServletRequestParameterException(MissingServletRequestParameterException exception) {
    return toError(exception, error -> {
      Map<String, List<String>> errors = Map.of(exception.getParameterName(), List.of("missing"));
      Response<Object> response = Response.failed(ResponseCode.BAD_REQUEST, errors);
      return Pair.of(errors, response);
    });
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(HandlerMethodValidationException.class)
  public Response<Object> handlerMethodValidationException(HandlerMethodValidationException exception) {
    return toError(exception, error -> {
      List<String> errorKeys = error.getParameterValidationResults()
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
      return Pair.of(errors, response);
    });
  }

  private Response<Object> toBaseError(Throwable throwable) {
    return toError(throwable, error -> {
      Map<String, List<String>> errors = Map.of(EXCEPTION_KEY, List.of(error.getMessage()));
      Response<Object> response = Response.failed(ResponseCode.INTERNAL_ERROR,
          Collections.emptyMap());
      return Pair.of(errors, response);
    });
  }

  private <S extends Throwable> Response<Object> toError(S source,
      Function<S, Pair<Map<String, List<String>>, Response<Object>>> function) {
    log.error(AppLogMessage.message(ERROR_LOG).error(source));

    Pair<Map<String, List<String>>, Response<Object>> pair = function.apply(source);
    Map<String, List<String>> errorList = pair.getLeft();
    Response<Object> response = pair.getRight();

    Map<String, List<String>> errors = Optional.ofNullable(response)
        .map(Response::getError)
        .map(ErrorMetadata::getViolations)
        .orElseGet(() -> errorList);
    ContextHelper.put(getErrors(errors));
    observationHelper.decorateErrorObservation(
        observationWrapper,
        source,
        ResponseCode.fromCode(response.getResponse().getCode())
    );

    return response;
  }

  private String getErrors(Map<String, List<String>> violations) {
    try {
      return objectMapper.writeValueAsString(violations);
    } catch (JacksonException e) {
      log.error(AppLogMessage.message("#ApiError - failed convert errors").error(e));
      return null;
    }
  }

  private Map<String, List<String>> from(MethodArgumentNotValidException exception) {
    BindingResult result = exception.getBindingResult();
    if (!result.hasFieldErrors()) {
      return Collections.emptyMap();
    }

    Map<String, Set<String>> map = new HashMap<>();

    for (FieldError fieldError : result.getFieldErrors()) {
      String field = fieldError.getField();

        if (!map.containsKey(field)) {
          map.put(field, new TreeSet<>());
        }

        String errorMessage = fieldError.getDefaultMessage();
        map.get(field).add(errorMessage);
      }

      return map.entrySet()
          .stream()
          .collect(Collectors.toMap(
              Entry::getKey,
              entry -> new LinkedList<>(entry.getValue())
          ));
    }
  }