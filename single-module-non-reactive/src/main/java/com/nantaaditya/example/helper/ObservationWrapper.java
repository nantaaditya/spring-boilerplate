package com.nantaaditya.example.helper;

import io.micrometer.context.ContextSnapshot;
import io.micrometer.observation.Observation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ObservationWrapper {

  private static final String REQUEST_ATTR = "id.alto.cms.observation";

  public void setObservation(HttpServletRequest request, Observation observation) {
    if (request != null) {
      request.setAttribute(REQUEST_ATTR, observation);
    }
  }

  public Observation getObservation(HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    Object value = request.getAttribute(REQUEST_ATTR);
    return value instanceof Observation o ? o : null;
  }

  public void clear(HttpServletRequest request) {
    if (request != null) {
      request.removeAttribute(REQUEST_ATTR);
    }
  }

  /**
   * Propagates Micrometer tracing context to an async thread.
   * Use for tasks with no originating HTTP request (e.g. scheduled jobs).
   * Observation is NOT propagated — use {@link #wrap(Runnable, HttpServletRequest)}
   * when a request context is available.
   */
  public Runnable wrap(Runnable runnable) {
    ContextSnapshot snapshot = captureSnapshot();
    return () -> {
      try (ContextSnapshot.Scope scope = snapshot.setThreadLocals()) {
        runnable.run();
      }
    };
  }

  /**
   * Propagates both Micrometer tracing context and the current request's observation
   * to an async thread. The observation is opened as a scope on the worker thread so
   * that metrics and traces emitted inside the task are attributed to the originating request.
   */
  public Runnable wrap(Runnable runnable, HttpServletRequest request) {
    ContextSnapshot snapshot = captureSnapshot();
    Observation observation = getObservation(request);
    return () -> {
      try (ContextSnapshot.Scope scope = snapshot.setThreadLocals()) {
        if (observation != null) {
          try (Observation.Scope obsScope = observation.openScope()) {
            runnable.run();
          }
        } else {
          runnable.run();
        }
      }
    };
  }

  private ContextSnapshot captureSnapshot() {
    return ContextSnapshot.captureAll(); //NOSONAR
  }
}
