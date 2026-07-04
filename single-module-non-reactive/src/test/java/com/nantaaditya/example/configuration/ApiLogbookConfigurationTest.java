package com.nantaaditya.example.configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.nantaaditya.example.properties.LogProperties;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class ApiLogbookConfigurationTest {

  @InjectMocks
  private ApiLogbookConfiguration configuration;

  @Mock
  private Gson gson;

  @Mock
  private LogProperties logProperties;

  private final ObjectMapper objectMapper = JsonMapper.builder().build();

  @Test
  void logbook_returnsNonNull() {
    when(logProperties.getSensitiveFields()).thenReturn(Set.of("authorization"));

    assertNotNull(configuration.logbook(objectMapper, gson, logProperties));
  }
}
