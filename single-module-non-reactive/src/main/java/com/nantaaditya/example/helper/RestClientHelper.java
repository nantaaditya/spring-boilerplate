package com.nantaaditya.example.helper;

import java.util.Set;
import org.springframework.web.client.RestTemplate;

public interface RestClientHelper {
  RestTemplate getRestClient(String clientName);
  Set<String> getClientNames();
}
