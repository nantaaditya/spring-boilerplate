package com.nantaaditya.example.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nantaaditya.example.helper.ContextHelper;
import com.nantaaditya.example.helper.ObservationHelper;
import com.nantaaditya.example.helper.ObservationWrapper;
import com.nantaaditya.example.model.constant.ContextConstant;
import com.nantaaditya.example.model.constant.HeaderConstant;
import com.nantaaditya.example.model.dto.CacheBodyRequest;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HeaderFilterTest {

  private HeaderFilter filter;

  @Mock
  private ObservationHelper observationHelper;

  @Mock
  private ObservationWrapper observationWrapper;

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private FilterChain filterChain;

  @BeforeEach
  void setUp() {
    filter = new HeaderFilter();
    ReflectionTestUtils.setField(filter, "observationHelper", observationHelper);
    ReflectionTestUtils.setField(filter, "observationWrapper", observationWrapper);
    ReflectionTestUtils.setField(filter, "contextPath", "");

    request = new MockHttpServletRequest("GET", "/api/test");
    request.addHeader(HeaderConstant.CLIENT_ID.getHeader(), "client-1");
    request.addHeader(HeaderConstant.REQUEST_ID.getHeader(), "req-1");
    response = new MockHttpServletResponse();
    filterChain = mock(FilterChain.class);

    when(observationHelper.getObservationRegistry()).thenReturn(ObservationRegistry.NOOP);
  }

  @AfterEach
  void tearDown() {
    ContextHelper.cleanUp();
  }

  @Test
  void doFilterInternal_happyPath_decoratesResponseAndStopsObservation() throws Exception {
    Observation observation = mock(Observation.class);
    Observation.Scope scope = mock(Observation.Scope.class);
    when(observation.openScope()).thenReturn(scope);
    when(observation.isNoop()).thenReturn(false);

    try (MockedStatic<Observation> mockedObservation = mockStatic(Observation.class)) {
      mockedObservation.when(() -> Observation.start(anyString(), any(), any(ObservationRegistry.class)))
          .thenReturn(observation);

      filter.doFilterInternal(request, response, filterChain);
    }

    assertEquals("client-1", response.getHeader(HeaderConstant.CLIENT_ID.getHeader()));
    assertEquals("req-1", response.getHeader(HeaderConstant.REQUEST_ID.getHeader()));

    verify(filterChain).doFilter(any(HttpServletRequest.class), eq(response));
    verify(observationWrapper).setObservation(any(HttpServletRequest.class), eq(observation));
    verify(observationWrapper).clear(any(HttpServletRequest.class));
    verify(observation).stop();
    verify(observationHelper, never()).decorateResponseObservation(eq(observation), any(Throwable.class), any(String.class));
  }

  @Test
  void doFilterInternal_propagatesMdcContextIntoFilterChain() throws Exception {
    Observation observation = mock(Observation.class);
    Observation.Scope scope = mock(Observation.Scope.class);
    when(observation.openScope()).thenReturn(scope);
    when(observation.isNoop()).thenReturn(false);

    doAnswer(invocation -> {
      assertEquals("req-1", MDC.get(ContextConstant.REQUEST_ID.getValue()));
      return null;
    }).when(filterChain).doFilter(any(), any());

    try (MockedStatic<Observation> mockedObservation = mockStatic(Observation.class)) {
      mockedObservation.when(() -> Observation.start(anyString(), any(), any(ObservationRegistry.class)))
          .thenReturn(observation);

      filter.doFilterInternal(request, response, filterChain);
    }

    verify(filterChain).doFilter(any(HttpServletRequest.class), eq(response));
  }

  @Test
  void doFilterInternal_filterChainThrows_decoratesErrorClearsAndRethrows() throws Exception {
    Observation observation = mock(Observation.class);
    Observation.Scope scope = mock(Observation.Scope.class);
    when(observation.openScope()).thenReturn(scope);
    when(observation.isNoop()).thenReturn(false);

    RuntimeException failure = new RuntimeException("boom");
    doThrow(failure).when(filterChain).doFilter(any(), any());

    RuntimeException thrown;
    try (MockedStatic<Observation> mockedObservation = mockStatic(Observation.class)) {
      mockedObservation.when(() -> Observation.start(anyString(), any(), any(ObservationRegistry.class)))
          .thenReturn(observation);

      thrown = assertThrows(RuntimeException.class,
          () -> filter.doFilterInternal(request, response, filterChain));
    }

    assertSame(failure, thrown);
    verify(observationHelper).decorateResponseObservation(eq(observation), eq(failure), ArgumentMatchers.<String>isNull());
    verify(observationWrapper).clear(any(HttpServletRequest.class));
    verify(observation).stop();
  }

  @Test
  void doFilterInternal_noopObservation_skipsStop() throws Exception {
    Observation observation = mock(Observation.class);
    Observation.Scope scope = mock(Observation.Scope.class);
    when(observation.openScope()).thenReturn(scope);
    when(observation.isNoop()).thenReturn(true);

    try (MockedStatic<Observation> mockedObservation = mockStatic(Observation.class)) {
      mockedObservation.when(() -> Observation.start(anyString(), any(), any(ObservationRegistry.class)))
          .thenReturn(observation);

      filter.doFilterInternal(request, response, filterChain);
    }

    verify(observation, never()).stop();
    verify(observationWrapper).clear(any(HttpServletRequest.class));
  }

  @Test
  void doFilterInternal_wrapsRequestWithCacheBodyRequest() throws Exception {
    Observation observation = mock(Observation.class);
    Observation.Scope scope = mock(Observation.Scope.class);
    when(observation.openScope()).thenReturn(scope);
    when(observation.isNoop()).thenReturn(true);

    ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);

    try (MockedStatic<Observation> mockedObservation = mockStatic(Observation.class)) {
      mockedObservation.when(() -> Observation.start(anyString(), any(), any(ObservationRegistry.class)))
          .thenReturn(observation);

      filter.doFilterInternal(request, response, filterChain);
    }

    verify(filterChain).doFilter(requestCaptor.capture(), eq(response));
    assertInstanceOf(CacheBodyRequest.class, requestCaptor.getValue());
  }
}
