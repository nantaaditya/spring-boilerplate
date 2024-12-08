package com.nantaaditya.example.helper;

import java.io.FileReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

@Slf4j
public class MavenHelper {

  private MavenHelper() {}

  public static Model getMavenModel() {
    try {
      MavenXpp3Reader reader = new MavenXpp3Reader();
      Model model = reader.read(new FileReader("pom.xml"));
      return model;
    } catch (Exception ex) {
      return null;
    }
  }
}
