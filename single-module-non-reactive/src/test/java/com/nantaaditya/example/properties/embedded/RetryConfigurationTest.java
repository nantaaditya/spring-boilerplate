package com.nantaaditya.example.properties.embedded;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nantaaditya.example.model.constant.BackoffPolicyConstant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RetryConfigurationTest {

  private RetryConfiguration config(String retryableExceptions) {
    return new RetryConfiguration(BackoffPolicyConstant.FIXED, 1000L, 2.0, 30000L, 3, retryableExceptions);
  }

  @Test
  void getWhitelistedExceptions_nullExceptions_returnsEmpty() {
    List<Class<? extends Throwable>> result = config(null).getWhitelistedExceptions();
    assertTrue(result.isEmpty());
  }

  @Test
  void getBlacklistedExceptions_nullExceptions_returnsEmpty() {
    List<Class<? extends Throwable>> result = config(null).getBlacklistedExceptions();
    assertTrue(result.isEmpty());
  }

  @Test
  void getWhitelistedExceptions_runtimeException_returnsOne() {
    List<Class<? extends Throwable>> result = config("java.lang.RuntimeException:true").getWhitelistedExceptions();
    assertEquals(1, result.size());
    assertEquals(RuntimeException.class, result.getFirst());
  }

  @Test
  void getBlacklistedExceptions_runtimeException_returnsOne() {
    List<Class<? extends Throwable>> result = config("java.lang.RuntimeException:false").getBlacklistedExceptions();
    assertEquals(1, result.size());
    assertEquals(RuntimeException.class, result.getFirst());
  }

  @Test
  void getWhitelistedExceptions_nonThrowableClass_skipped() {
    List<Class<? extends Throwable>> result = config("java.lang.String:true").getWhitelistedExceptions();
    assertTrue(result.isEmpty());
  }

  @Test
  void getWhitelistedExceptions_invalidToken_skipped() {
    List<Class<? extends Throwable>> result = config("badformat").getWhitelistedExceptions();
    assertTrue(result.isEmpty());
  }

  @Test
  void getWhitelistedExceptions_classNotFound_skipped() {
    List<Class<? extends Throwable>> result = config("com.example.NonExistentClass:true").getWhitelistedExceptions();
    assertTrue(result.isEmpty());
  }

  @Test
  void getWhitelistedExceptions_mixedList_returnsOnlyWhitelisted() {
    String exceptions = "java.lang.RuntimeException:true,java.io.IOException:false";
    List<Class<? extends Throwable>> result = config(exceptions).getWhitelistedExceptions();
    assertEquals(1, result.size());
    assertEquals(RuntimeException.class, result.getFirst());
  }
}
