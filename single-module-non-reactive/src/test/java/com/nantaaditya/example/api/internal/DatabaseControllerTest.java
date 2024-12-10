package com.nantaaditya.example.api.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nantaaditya.example.model.constant.ResponseCode;
import com.nantaaditya.example.model.response.Response;
import com.nantaaditya.example.service.internal.DatabaseMigrationService;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatabaseControllerTest {
  @InjectMocks
  private DatabaseController controller;
  @Mock
  private DatabaseMigrationService databaseMigrationService;

  @Test
  void migrate_success() {
    MigrateResult migrateResult = new MigrateResult();
    migrateResult.setSuccess(true);

    when(databaseMigrationService.migrate())
        .thenReturn(migrateResult);
    Response<MigrateResult> result = controller.migrate();
    assertNotNull(result);
    assertEquals(ResponseCode.SUCCESS.getCode(), result.getResponse().getCode());
    verify(databaseMigrationService).migrate();
  }

  @Test
  void migrate_failed() {
    MigrateResult migrateResult = new MigrateResult();
    migrateResult.setSuccess(false);

    when(databaseMigrationService.migrate())
        .thenReturn(migrateResult);
    Response<MigrateResult> result = controller.migrate();
    assertNotNull(result);
    assertEquals(ResponseCode.INTERNAL_ERROR.getCode(), result.getResponse().getCode());
    verify(databaseMigrationService).migrate();
  }
}