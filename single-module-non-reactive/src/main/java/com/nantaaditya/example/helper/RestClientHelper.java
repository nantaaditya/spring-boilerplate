package com.nantaaditya.example.helper;

import java.util.Set;
import org.springframework.web.client.RestClient;

public interface RestClientHelper {
  RestClient getRestClient(String clientName);
  Set<String> getClientNames();
}
