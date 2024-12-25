package com.nantaaditya.example.configuration;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nantaaditya.example.properties.AsyncTaskProperties;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsyncTaskConfigurationTest {

  @InjectMocks
  private AsyncTaskConfiguration asyncTaskConfiguration;

  @Mock
  private AsyncTaskProperties asyncTaskProperties;

  @Test
  void onStart_null() {
    when(asyncTaskProperties.configurations())
        .thenReturn(null);

    asyncTaskConfiguration.onStart();

    verify(asyncTaskProperties).configurations();
  }

  @Test
  void onStart_empty() {
    when(asyncTaskProperties.configurations())
        .thenReturn(Collections.emptyMap());

    asyncTaskConfiguration.onStart();

    verify(asyncTaskProperties, atLeast(1)).configurations();
  }
}