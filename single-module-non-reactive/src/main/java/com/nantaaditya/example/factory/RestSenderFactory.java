package com.nantaaditya.example.factory;

import com.nantaaditya.example.helper.RestSender;
import com.nantaaditya.example.helper.RestSenderHelper;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.FactoryBean;

@Setter
public class RestSenderFactory implements FactoryBean<RestSenderHelper> {

  private Map<String, RestSender> restSenders = new HashMap<>();

  @Override
  public RestSenderHelper getObject() throws Exception {
    return new RestSenderHelperImpl(restSenders);
  }

  @Override
  public Class<?> getObjectType() {
    return RestSenderHelper.class;
  }

  @AllArgsConstructor
  private static class RestSenderHelperImpl implements RestSenderHelper {
    private static final String POSTFIX_BEAN_NAME = "RestSender";

    private Map<String, RestSender> restSenders = new HashMap<>();

    @Override
    public RestSender getRestSender(String clientName) {
      return restSenders.get(clientName + POSTFIX_BEAN_NAME);
    }
  }
}
