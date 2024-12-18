package com.nantaaditya.example.service.internal;

import com.nantaaditya.example.model.request.RetryDeadLetterProcessRequest;

public interface DeadLetterProcessService {
    void remove(int days);

    void retry(RetryDeadLetterProcessRequest request);
}
