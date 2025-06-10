package com.nantaaditya.example.model.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class WebClientPayloadRequest extends WebClientRequest {
  private Object requestBody;
}
