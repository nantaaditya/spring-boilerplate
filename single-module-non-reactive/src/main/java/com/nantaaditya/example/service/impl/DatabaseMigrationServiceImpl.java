package com.nantaaditya.example.service.impl;

import com.nantaaditya.example.service.internal.DatabaseMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseMigrationServiceImpl implements DatabaseMigrationService {

  private final Flyway flyway;

  @Override
  public MigrateResult migrate() {
    return flyway.migrate();
  }
}
