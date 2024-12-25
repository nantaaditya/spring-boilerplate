package com.nantaaditya.example.helper;

import org.springframework.retry.support.RetryTemplate;

public interface RetryTemplateHelper {
  RetryTemplate getRetryTemplate(String retryTemplateName);
}
