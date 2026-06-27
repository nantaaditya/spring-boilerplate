package com.nantaaditya.example.helper;

import org.springframework.core.retry.RetryTemplate;

public interface RetryTemplateHelper {
  RetryTemplate getRetryTemplate(String retryTemplateName);
}
