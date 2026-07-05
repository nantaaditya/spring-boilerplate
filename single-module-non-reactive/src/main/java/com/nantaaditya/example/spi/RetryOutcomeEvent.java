package com.nantaaditya.example.spi;

import java.time.LocalDateTime;

/**
 * {@code idempotencyKey} is a best-effort correlation id whose source differs by {@link #source()}:
 * the request id for {@link RetryExhaustionSource#INITIAL_RETRY}, the dead-letter record's actual
 * idempotency key for {@link RetryExhaustionSource#DEAD_LETTER_REPLAY}.
 */
public record RetryOutcomeEvent(
    String processType,
    String processName,
    String idempotencyKey,
    int retryCount,
    int maxRetry,
    String lastError,
    RetryExhaustionSource source,
    LocalDateTime occurredAt
) {

}
