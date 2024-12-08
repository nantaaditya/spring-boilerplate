package com.nantaaditya.example.entity.generator;

import com.nantaaditya.example.helper.TsidHelper;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

public class TsidGenerator implements IdentifierGenerator {

  @Override
  public Object generate(SharedSessionContractImplementor session, Object object)
      throws HibernateException {
    return TsidHelper.generateTsid();
  }
}
