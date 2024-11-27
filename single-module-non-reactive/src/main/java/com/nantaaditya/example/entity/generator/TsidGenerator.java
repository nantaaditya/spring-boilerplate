package com.nantaaditya.example.entity.generator;

import com.nantaaditya.example.helper.TsidHelper;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

public class TsidGenerator extends SequenceStyleGenerator {

  @Override
  public Object generate(SharedSessionContractImplementor session, Object object)
      throws HibernateException {
    return TsidHelper.generateTsid();
  }
}
