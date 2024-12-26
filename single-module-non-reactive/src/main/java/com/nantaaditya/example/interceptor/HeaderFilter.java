package com.nantaaditya.example.interceptor;

import com.nantaaditya.example.helper.ContextHelper;
import com.nantaaditya.example.helper.DateTimeHelper;
import com.nantaaditya.example.model.constant.HeaderConstant;
import com.nantaaditya.example.model.dto.CacheBodyRequest;
import com.nantaaditya.example.model.dto.ContextDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class HeaderFilter extends OncePerRequestFilter {

  @Value("${server.servlet.context-path}")
  private String contextPath;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    HttpServletRequest httpServletRequest = new CacheBodyRequest(request);

    ContextDTO contextDTO = new ContextDTO(
        httpServletRequest.getHeader(HeaderConstant.CLIENT_ID.getHeader()),
        httpServletRequest.getHeader(HeaderConstant.REQUEST_ID.getHeader()),
        httpServletRequest.getMethod(),
        httpServletRequest.getRequestURI().replace(contextPath, ""),
        httpServletRequest.getHeader(HeaderConstant.REQUEST_TIME.getHeader()),
        DateTimeHelper.getDateInFormat(ZonedDateTime.now(), DateTimeHelper.ISO_8601_GMT7_FORMAT),
        null, null, null
    );

    ContextHelper.put(contextDTO);

    response.addHeader(HeaderConstant.CLIENT_ID.getHeader(), contextDTO.clientId());
    response.addHeader(HeaderConstant.REQUEST_ID.getHeader(), contextDTO.requestId());
    response.addHeader(HeaderConstant.REQUEST_TIME.getHeader(), contextDTO.receivedTime());
    response.addHeader(HeaderConstant.RECEIVED_TIME.getHeader(), contextDTO.receivedTime());

    filterChain.doFilter(httpServletRequest, response);
  }
}
