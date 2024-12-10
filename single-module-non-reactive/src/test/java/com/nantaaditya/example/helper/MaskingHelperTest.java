package com.nantaaditya.example.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.Gson;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MaskingHelperTest {

  @Test
  void masking_null() {
    assertNull(MaskingHelper.masking(null));
  }

  @Test
  void masking_lessThan2Char() {
    assertEquals("ab", MaskingHelper.masking("ab"));
  }

  @Test
  void masking_lengthLessThanEligible() {
    assertEquals("abc", MaskingHelper.masking("abc", 3, 3));
  }

  @Test
  void maskingJson_empty() {
   assertEquals("", MaskingHelper.maskingJson(new Gson(), Set.of(), ""));
  }

  @Test
  void masking_object() {
    String json = """
        {"name":"Johnson", "array":[{"k":"v"}]}
        """;
    assertEquals("{\"name\":\"Jo***on\",\"array\":[{\"k\":\"v\"}]}", MaskingHelper.maskingJson(new Gson(), Set.of("name"), json));
  }

  @Test
  void masking_array() {
    String json = """
        [{"name":"Johnson","other":{"key":"value"}}]
        """;
    assertEquals("[{\"name\":\"Jo***on\",\"other\":{\"key\":\"value\"}}]", MaskingHelper.maskingJson(new Gson(), Set.of("name"), json));
  }
}