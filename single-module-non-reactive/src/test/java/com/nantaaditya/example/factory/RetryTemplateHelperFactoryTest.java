package com.nantaaditya.example.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import com.nantaaditya.example.helper.RetryTemplateHelper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.retry.RetryTemplate;

class RetryTemplateHelperFactoryTest {

  private RetryTemplateHelperFactory factory;

  @BeforeEach
  void setUp() {
    factory = new RetryTemplateHelperFactory();
  }

  @Test
  void getObjectType_returnsRetryTemplateHelperClass() {
    assertEquals(RetryTemplateHelper.class, factory.getObjectType());
  }

  @Test
  void getObject_withEmptyMap_returnsHelper() throws Exception {
    assertNotNull(factory.getObject());
  }

  @Test
  void getRetryTemplate_existingTemplate_returnsIt() throws Exception {
    RetryTemplate mockTemplate = mock(RetryTemplate.class);
    factory.setRetryTemplates(Map.of("serviceARetryTemplate", mockTemplate));

    RetryTemplateHelper helper = factory.getObject();

    assertEquals(mockTemplate, helper.getRetryTemplate("serviceA"));
  }

  @Test
  void getRetryTemplate_unknownTemplate_returnsNull() throws Exception {
    factory.setRetryTemplates(Map.of("serviceARetryTemplate", mock(RetryTemplate.class)));

    RetryTemplateHelper helper = factory.getObject();

    assertNull(helper.getRetryTemplate("unknown"));
  }
}
