package com.nantaaditya.example.interceptor;

import com.nantaaditya.example.helper.ContextHelper;
import com.nantaaditya.example.helper.DateTimeHelper;
import com.nantaaditya.example.helper.ObservationHelper;
import com.nantaaditya.example.model.constant.HeaderConstant;
import com.nantaaditya.example.model.constant.ObservationConstant;
import com.nantaaditya.example.model.dto.CacheBodyRequest;
import com.nantaaditya.example.model.dto.ContextDTO;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Event;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class HeaderFilter extends OncePerRequestFilter {

  @Autowired
  private ObservationHelper observationHelper;

  @Value("${server.servlet.context-path}")
  private String contextPath;

  private static final String ERROR_KEY = "error";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    HttpServletRequest httpServletRequest = new CacheBodyRequest(request);

    ContextDTO context = createContext(httpServletRequest);
    ContextHelper.put(context);
    decorateHeaderResponse(response, context);

    Observation observation = Observation.start(
        ObservationConstant.from(context.path()).getName(),
        () -> observationHelper.createApiContext(context),
        observationHelper.getObservationRegistry()
    );

    Map<String, String> contextMap = MDC.getCopyOfContextMap();

    try (Observation.Scope scope = observation.openScope()) {
      MDC.setContextMap(contextMap);
      filterChain.doFilter(httpServletRequest, response);
    } catch (Exception ex) {
      log.error("#Observation - error {}", ex.getMessage());
      decorateErrorObservation(ex, observation);
      throw ex;
    } finally {
      observation.stop();
    }
  }

  private ContextDTO createContext(HttpServletRequest httpServletRequest) {
    return new ContextDTO(
        httpServletRequest.getHeader(HeaderConstant.CLIENT_ID.getHeader()),
        httpServletRequest.getHeader(HeaderConstant.REQUEST_ID.getHeader()),
        httpServletRequest.getMethod(),
        httpServletRequest.getRequestURI().replace(contextPath, ""),
        httpServletRequest.getHeader(HeaderConstant.REQUEST_TIME.getHeader()),
        DateTimeHelper.getDateInFormat(ZonedDateTime.now(), DateTimeHelper.ISO_8601_GMT7_FORMAT),
        null, null, null
    );
  }

  private void decorateHeaderResponse(HttpServletResponse response, ContextDTO contextDTO) {
    response.addHeader(HeaderConstant.CLIENT_ID.getHeader(), contextDTO.clientId());
    response.addHeader(HeaderConstant.REQUEST_ID.getHeader(), contextDTO.requestId());
    response.addHeader(HeaderConstant.REQUEST_TIME.getHeader(), contextDTO.receivedTime());
    response.addHeader(HeaderConstant.RECEIVED_TIME.getHeader(), contextDTO.receivedTime());
  }

  private void decorateErrorObservation(Exception ex, Observation observation) {
    String exceptionClass = ex.getClass().getName();
    observation.getContext().addLowCardinalityKeyValue(KeyValue.of(ERROR_KEY, exceptionClass));
    observation.event(Event.of(ERROR_KEY, exceptionClass));
    observation.error(ex);
  }
}

