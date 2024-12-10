package com.nantaaditya.example.helper;

import java.io.FileReader;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

@Slf4j
public class MavenHelper {

  private static final MavenXpp3Reader reader = new MavenXpp3Reader();

  private MavenHelper() {}

  @SneakyThrows
  public static Model getMavenModel() {
    return reader.read(new FileReader("pom.xml"));
  }
}
