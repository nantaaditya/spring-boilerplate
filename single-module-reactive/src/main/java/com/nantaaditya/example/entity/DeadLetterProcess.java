package com.nantaaditya.example.entity;

import io.r2dbc.spi.Row;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table(name = "dead_letter_process")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("java:S1068")
public class DeadLetterProcess extends BaseEntity<Long> {
  private String processType;
  private String processName;
  @Column
  private String lastError;
  @Column
  private byte[] payload;
  private boolean processed;

  public static DeadLetterProcess from(Row row) {
    return DeadLetterProcess.builder()
        .id(row.get("id", Long.class))
        .createdBy(row.get("created_by", String.class))
        .createdDate(row.get("created_date", Long.class))
        .updatedBy(row.get("updated_by", String.class))
        .updatedDate(row.get("updated_date", Long.class))
        .version(row.get("version", Long.class))
        .processType(row.get("process_type", String.class))
        .processName(row.get("process_name", String.class))
        .lastError(row.get("last_error", String.class))
        .payload(row.get("payload", byte[].class))
        .processed(row.get("processed", Boolean.class))
        .build();
  }
}
