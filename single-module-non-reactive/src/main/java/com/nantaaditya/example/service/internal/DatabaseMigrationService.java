package com.nantaaditya.example.service.internal;

import org.flywaydb.core.api.output.MigrateResult;

public interface DatabaseMigrationService {
  MigrateResult migrate();
}
