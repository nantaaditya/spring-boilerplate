package com.nantaaditya.example.helper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nantaaditya.example.model.constant.ContextConstant;
import com.nantaaditya.example.model.dto.ContextDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class ContextHelperTest {

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void put_contextDTO_setsMdcRequestIdAndContext() {
    ContextDTO dto = new ContextDTO("client-1", "req-1", "GET", "/api", "t1", "t2", "200", "OK", "t3");

    ContextHelper.put(dto);

    assertEquals("req-1", MDC.get("requestId"));
    assertNotNull(MDC.get("context"));
  }

  @Test
  void get_afterPut_returnsDto() {
    ContextDTO dto = new ContextDTO("client-1", "req-1", "GET", "/api", "t1", "t2", "200", "OK", "t3");
    ContextHelper.put(dto);

    ContextDTO result = ContextHelper.get();

    assertNotNull(result);
    assertEquals("req-1", result.requestId());
    assertEquals("client-1", result.clientId());
  }

  @Test
  void get_whenEmpty_returnsNull() {
    assertNull(ContextHelper.get());
  }

  @Test
  void get_contextConstant_allValues_returnsCorrectFields() {
    ContextDTO dto = new ContextDTO("cid", "rid", "GET", "/path", "req-t", "recv-t", "200", "OK", "resp-t");
    ContextHelper.put(dto);

    assertEquals("cid", ContextHelper.get(ContextConstant.CLIENT_ID));
    assertEquals("rid", ContextHelper.get(ContextConstant.REQUEST_ID));
    assertEquals("req-t", ContextHelper.get(ContextConstant.REQUEST_TIME));
    assertEquals("recv-t", ContextHelper.get(ContextConstant.RECEIVED_TIME));
    assertEquals("200", ContextHelper.get(ContextConstant.RESPONSE_CODE));
    assertEquals("OK", ContextHelper.get(ContextConstant.RESPONSE_DESCRIPTION));
    assertEquals("resp-t", ContextHelper.get(ContextConstant.RESPONSE_TIME));
  }

  @Test
  void get_contextConstant_whenNoContext_returnsNull() {
    assertNull(ContextHelper.get(ContextConstant.REQUEST_ID));
  }

  @Test
  void getRequestId_afterPut_returnsRequestId() {
    ContextDTO dto = new ContextDTO("c", "req-123", "GET", "/", "t1", "t2", "200", "OK", "t3");
    ContextHelper.put(dto);

    assertEquals("req-123", ContextHelper.getRequestId());
  }

  @Test
  void getRequestId_whenEmpty_returnsNull() {
    assertNull(ContextHelper.getRequestId());
  }

  @Test
  void putString_setsAdditionalDataInMdc() {
    ContextHelper.put("extra-data");

    byte[] data = ContextHelper.getAdditionalData();

    assertNotNull(data);
    assertArrayEquals("extra-data".getBytes(), data);
  }

  @Test
  void getAdditionalData_whenNotSet_returnsNull() {
    assertNull(ContextHelper.getAdditionalData());
  }

  @Test
  void update_appliesFunction_updatesContext() {
    ContextDTO initial = new ContextDTO("c", "r", "GET", "/api", "t1", "t2", null, null, null);
    ContextHelper.put(initial);

    ContextHelper.update(ctx -> ctx.withDescription("updated-desc"));

    ContextDTO result = ContextHelper.get();
    assertNotNull(result);
    assertEquals("updated-desc", result.responseDescription());
  }

  @Test
  void update_whenNoContext_isNoOp() {
    assertDoesNotThrow(() -> ContextHelper.update(ctx -> ctx));
  }

  @Test
  void cleanUp_clearsMdcEntries() {
    ContextHelper.put("some-data");

    ContextHelper.cleanUp();

    assertNull(MDC.get("additionalData"));
    assertNull(MDC.get("context"));
  }
}
