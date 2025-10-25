package com.nantaaditya.example.service.impl;

import com.nantaaditya.example.service.internal.DatabaseMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true")
public class DatabaseMigrationServiceImpl implements DatabaseMigrationService {

  private final Flyway flyway;

  @Override
  public MigrateResult migrate() {
    return flyway.migrate();
  }
}
