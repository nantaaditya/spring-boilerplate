package com.nantaaditya.example.service.internal;

import com.nantaaditya.example.model.request.RetryDeadLetterProcessRequest;

public interface DeadLetterProcessService {
    void remove(int days);

    void retry(RetryDeadLetterProcessRequest request);

    // Unlike retry(), which only queries NEW/FAILED records, retryById allows RETRYING records
    // to pass through — enabling force-retry of records stuck in RETRYING after a crashed run.
    // SUCCESS and EXHAUSTED records are still blocked.
    void retryById(long id);
}
