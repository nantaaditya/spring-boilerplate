package com.nantaaditya.example.interceptor;

import com.nantaaditya.example.helper.ContextHelper;
import com.nantaaditya.example.helper.DateTimeHelper;
import com.nantaaditya.example.model.constant.HeaderConstant;
import com.nantaaditya.example.model.dto.ContextDTO;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HeaderFilter implements Filter {

  @Value("${server.servlet.context-path}")
  private String contextPath;

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
      FilterChain filterChain) throws IOException, ServletException {

    HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
    HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

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

    httpServletResponse.addHeader(HeaderConstant.CLIENT_ID.getHeader(), contextDTO.clientId());
    httpServletResponse.addHeader(HeaderConstant.REQUEST_ID.getHeader(), contextDTO.requestId());
    httpServletResponse.addHeader(HeaderConstant.REQUEST_TIME.getHeader(), contextDTO.receivedTime());
    httpServletResponse.addHeader(HeaderConstant.RECEIVED_TIME.getHeader(), contextDTO.receivedTime());

    filterChain.doFilter(servletRequest, servletResponse);
  }
}
