package com.nantaaditya.example.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nantaaditya.example.helper.ContextHelper;
import com.nantaaditya.example.model.dto.ContextDTO;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JpaAuditorConfigurationTest {

  @InjectMocks
  private JpaAuditorConfiguration jpaAuditorConfiguration;

  @Test
  void getCurrentAuditor_empty() {
    assertEquals(Optional.of("SYSTEM"), jpaAuditorConfiguration.getCurrentAuditor());
  }

  @Test
  void getCurrentAuditor_clientId() {
    ContextDTO contextDTO = new ContextDTO("clientId", "reqId", null, null, null, null, null, null, null);
    ContextHelper.put(contextDTO);

    assertEquals(Optional.of("clientId"), jpaAuditorConfiguration.getCurrentAuditor());
  }
}