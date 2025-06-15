package com.nantaaditya.example.helper;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.StringTokenizer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StringHelper {

  private StringHelper() {}

  public static Collection<String> toCollection(String fields, String delimiter,
      Class<? extends Collection> collectionClass) {
    try {
      Collection<String> collections = (Collection<String>) collectionClass.getDeclaredConstructor().newInstance(); // NOSONAR

      if (fields == null || fields.isEmpty()) return collections;

      StringTokenizer tokenizer = delimiter == null || delimiter.isEmpty() ?
          new StringTokenizer(fields) : new StringTokenizer(fields, delimiter);

      while (tokenizer.hasMoreTokens()) {
        collections.add(tokenizer.nextToken().trim());
      }
      return collections;
    } catch (InstantiationException | IllegalAccessException
             | NoSuchMethodException | InvocationTargetException e) {
      throw new IllegalArgumentException("#Converter - error creating collection instance", e);
    }
  }
}
