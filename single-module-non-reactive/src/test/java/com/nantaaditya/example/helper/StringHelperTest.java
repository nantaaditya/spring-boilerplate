package com.nantaaditya.example.helper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StringHelperTest {

  @Test
  void toCollection_null() {
    Set<String> result = (HashSet<String>) StringHelper.toCollection(null, null, HashSet.class);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void toCollection_empty() {
    Set<String> result = (HashSet<String>) StringHelper.toCollection("", null, HashSet.class);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void toCollection_delimiterNull() {
    Set<String> result = (HashSet<String>) StringHelper.toCollection("a b c", null, HashSet.class);
    assertNotNull(result);
    assertFalse(result.isEmpty());
  }

  @Test
  void toCollection_delimiterEmpty() {
    Set<String> result = (HashSet<String>) StringHelper.toCollection("a b c", "", HashSet.class);
    assertNotNull(result);
    assertFalse(result.isEmpty());
  }
}