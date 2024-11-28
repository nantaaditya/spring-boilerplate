package com.nantaaditya.example.model.response;

import com.nantaaditya.example.model.request.ExampleTestRequest;
import com.nantaaditya.example.model.request.HeaderRequest;
import lombok.Data;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Component;

@Data
@Component
public class ContextResponse {
  private MockHttpServletResponse response = null;
  private HeaderRequest headerRequest = HeaderRequest.getInstance();
  private ExampleTestRequest exampleRequest = ExampleTestRequest.getInstance();
}
