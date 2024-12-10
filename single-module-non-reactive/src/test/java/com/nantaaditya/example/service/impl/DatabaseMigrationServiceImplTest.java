package com.nantaaditya.example.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatabaseMigrationServiceImplTest {
  @InjectMocks
  private DatabaseMigrationServiceImpl databaseMigrationService;
  @Mock
  private Flyway flyway;

  @Test
  void migrate() {
    when(flyway.migrate())
        .thenReturn(new MigrateResult());
    MigrateResult result = databaseMigrationService.migrate();
    assertNotNull(result);
    verify(flyway).migrate();
  }
}