package com.nantaaditya.example.factory;

import com.nantaaditya.example.helper.RetryTemplateHelper;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.retry.support.RetryTemplate;

@Setter
public class RetryTemplateHelperFactory implements FactoryBean<RetryTemplateHelper> {

  private Map<String, RetryTemplate> retryTemplates = new HashMap<>();

  @Override
  public RetryTemplateHelper getObject() throws Exception {
    return new RetryTemplateHelperImpl(retryTemplates);
  }

  @Override
  public Class<?> getObjectType() {
    return RetryTemplateHelper.class;
  }

  @AllArgsConstructor
  private static class RetryTemplateHelperImpl implements RetryTemplateHelper {
    private static final String POSTFIX_BEAN_NAME = "RetryTemplate";

    private Map<String, RetryTemplate> retryTemplates = new HashMap<>();

    @Override
    public RetryTemplate getRetryTemplate(String retryTemplateName) {
      return retryTemplates.get(retryTemplateName + POSTFIX_BEAN_NAME);
    }

  }
}
