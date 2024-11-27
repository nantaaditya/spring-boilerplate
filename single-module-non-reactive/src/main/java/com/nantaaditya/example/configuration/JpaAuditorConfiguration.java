package com.nantaaditya.example.configuration;

import com.nantaaditya.example.helper.ContextHelper;
import com.nantaaditya.example.model.dto.ContextDTO;
import java.util.Optional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

@Configuration
public class JpaAuditorConfiguration implements AuditorAware<String> {

  @Override
  public Optional<String> getCurrentAuditor() {
    ContextDTO context = ContextHelper.get();

    if (context == null) {
      return Optional.of("SYSTEM");
    }

    return Optional.ofNullable(context)
        .map(ContextDTO::clientId);
  }
}
