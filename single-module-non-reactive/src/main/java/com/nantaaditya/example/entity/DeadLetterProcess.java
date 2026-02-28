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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.retry.RetryContext;

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

  public static DeadLetterProcess create(RetryContext retryContext, byte[] request, byte[] retryHistories) {
    return DeadLetterProcess.builder()
        .processType((String) retryContext.getAttribute(RetryConstant.PROCESS_TYPE.getName()))
        .processName((String) retryContext.getAttribute(RetryConstant.PROCESS_NAME.getName()))
        .idempotencyKey((String) retryContext.getAttribute(RetryConstant.REQUEST_ID.getName()))
        .clientName((String) retryContext.getAttribute(RetryConstant.CLIENT_NAME.getName()))
        .method((String) retryContext.getAttribute(RetryConstant.METHOD.getName()))
        .path((String) retryContext.getAttribute(RetryConstant.PATH.getName()))
        .headers((String) retryContext.getAttribute(RetryConstant.HEADERS.getName()))
        .payload(request)
        .retryCount(0)
        .maxRetry((int) retryContext.getAttribute(RetryConstant.MAX_RETRY.getName()))
        .status(RetryStatus.NEW.name())
        .lastError(retryContext.getLastThrowable().getMessage())
        .retryHistories(retryHistories)
        .build();
  }
}
