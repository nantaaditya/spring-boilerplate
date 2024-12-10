package com.nantaaditya.example.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GsonHelperTest {

  @Test
  void cleanJson_array() {
    String json = """
        {"name":"Johnson", "array":[{"k":"v"}]}
        """;
    assertEquals("{\"name\":\"Johnson\",\"array\":[{\"k\":\"v\"}]}", GsonHelper.cleanJson(json, new Gson()));
  }

  @Test
  void cleanJson_object() {
    String json = """
        [{"name":"Johnson","other":{"key":"value"}}]
        """;
    assertEquals("[{\"name\":\"Johnson\",\"other\":{\"key\":\"value\"}}]", GsonHelper.cleanJson(json, new Gson()));
  }
}