package com.nantaaditya.example.entity;

import com.nantaaditya.example.model.constant.RetryConstant;
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
  @Column(columnDefinition = "TEXT")
  private String lastError;
  private byte[] payload;
  private boolean processed;

  public static DeadLetterProcess create(RetryContext retryContext, byte[] request) {
    return DeadLetterProcess.builder()
        .processType((String) retryContext.getAttribute(RetryConstant.PROCESS_TYPE.getName()))
        .processName((String) retryContext.getAttribute(RetryConstant.PROCESS_NAME.getName()))
        .payload(request)
        .lastError(retryContext.getLastThrowable().getMessage())
        .processed(false)
        .build();
  }
}
