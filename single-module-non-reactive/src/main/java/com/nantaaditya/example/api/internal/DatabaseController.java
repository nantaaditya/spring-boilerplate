package com.nantaaditya.example.api.internal;

import com.nantaaditya.example.model.constant.ResponseCode;
import com.nantaaditya.example.model.response.Response;
import com.nantaaditya.example.service.internal.DatabaseMigrationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/internal-api/database")
@Tag(name = "internal api", description = "internal api for utility purpose")
public class DatabaseController {

  private final DatabaseMigrationService databaseMigrationService;

  @PostMapping(value = "/migrate",
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public Response<MigrateResult> migrate() {
    MigrateResult migrateResult = databaseMigrationService.migrate();
    if (migrateResult.success) {
      return Response.success(migrateResult);
    }

    return Response.failed(ResponseCode.INTERNAL_ERROR, Map.of("migration", List.of("failed")));
  }
}
