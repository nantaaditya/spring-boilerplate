package com.nantaaditya.example.entity;

import com.nantaaditya.example.model.constant.RetryConstant;
import com.nantaaditya.example.model.constant.RetryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "dead_letter_process")
@EntityListeners(AuditingEntityListener.class)
@SuppressWarnings("java:S1068")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DeadLetterProcess extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;
  private String processType;
  private String processName;
  private String idempotencyKey;
  private String clientName;
  private String method;
  @Column(columnDefinition = "TEXT")
  private String path;
  private String headers;
  @Column(columnDefinition = "bytea")
  private byte[] payload;
  private int retryCount;
  private int maxRetry;
  private String status;
  @Column(columnDefinition = "TEXT")
  private String lastError;
  private byte[] retryHistories;

  public static DeadLetterProcess create(Map<String, Object> retryContext, byte[] request, byte[] retryHistories,
      Throwable ex) {
    return DeadLetterProcess.builder()
        .processType((String) retryContext.get(RetryConstant.PROCESS_TYPE.getName()))
        .processName((String) retryContext.get(RetryConstant.PROCESS_NAME.getName()))
        .idempotencyKey((String) retryContext.get(RetryConstant.REQUEST_ID.getName()))
        .clientName((String) retryContext.get(RetryConstant.CLIENT_NAME.getName()))
        .method((String) retryContext.get(RetryConstant.METHOD.getName()))
        .path((String) retryContext.get(RetryConstant.PATH.getName()))
        .headers((String) retryContext.get(RetryConstant.HEADERS.getName()))
        .payload(request)
        .retryCount(0)
        .maxRetry(retryContext.get(RetryConstant.MAX_RETRY.getName()) instanceof Integer i ? i : 0)
        .status(RetryStatus.NEW.name())
        .lastError(ex.getMessage())
        .retryHistories(retryHistories)
        .build();
  }
}
