package com.nantaaditya.example.configuration;

import com.nantaaditya.example.interceptor.EventLogInterceptor;
import com.nantaaditya.example.properties.LogProperties;
import com.nantaaditya.example.repository.EventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

  private final EventLogRepository eventLogRepository;
  private final LogProperties logProperties;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(new EventLogInterceptor(eventLogRepository, logProperties))
        .order(Ordered.LOWEST_PRECEDENCE);
  }
}
