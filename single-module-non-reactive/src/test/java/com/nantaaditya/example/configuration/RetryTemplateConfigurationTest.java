package com.nantaaditya.example.configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nantaaditya.example.properties.RetryProperties;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RetryTemplateConfigurationTest {

  @InjectMocks
  private RetryTemplateConfiguration retryTemplateConfiguration;

  @Mock
  private RetryProperties retryProperties;

  @Test
  void retryTemplateHelperFactory_null() {
    when(retryProperties.configurations())
        .thenReturn(null);

    assertNotNull(retryTemplateConfiguration.retryTemplateHelperFactory());

    verify(retryProperties).configurations();
  }

  @Test
  void retryTemplateHelperFactory_empty() {
    when(retryProperties.configurations())
        .thenReturn(Collections.emptyMap());

    assertNotNull(retryTemplateConfiguration.retryTemplateHelperFactory());

    verify(retryProperties, atLeast(1)).configurations();
  }
}