package com.nantaaditya.example.helper;

import com.github.f4b6a3.tsid.TsidCreator;

public class TsidHelper {

  private TsidHelper() {}

  public static String generateTsid() {
    return TsidCreator.getTsid256().toLowerCase();
  }
}
