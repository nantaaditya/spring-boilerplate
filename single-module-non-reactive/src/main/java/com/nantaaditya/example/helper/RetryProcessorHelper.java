package com.nantaaditya.example.helper;

import com.nantaaditya.example.service.impl.AbstractRetryProcessorService;

public interface RetryProcessorHelper {
  AbstractRetryProcessorService getProcessor(String processType, String processName);
}
