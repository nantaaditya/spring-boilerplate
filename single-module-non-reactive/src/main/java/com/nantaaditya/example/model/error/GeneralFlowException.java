package com.nantaaditya.example.model.error;

import com.nantaaditya.example.model.constant.ResponseCode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;

@Getter
@SuppressWarnings("java:S1068")
public class GeneralFlowException extends RuntimeException {

  private final ResponseCode response;
  private final Map<String, List<String>> violations = new HashMap<>();

  public GeneralFlowException(ResponseCode responseCode) {
    super(responseCode.getMessage());
    this.response = responseCode;
  }

  public GeneralFlowException(ResponseCode responseCode, Map<String, List<String>> violations) {
    super(responseCode.getMessage());
    this.response = responseCode;
    this.violations.putAll(violations);
  }

  public GeneralFlowException(String message, ResponseCode responseCode) {
    super(message);
    this.response = responseCode;
  }

  public GeneralFlowException(String message, ResponseCode responseCode, Map<String, List<String>> violations) {
    super(message);
    this.response = responseCode;
    this.violations.putAll(violations);
  }
}
