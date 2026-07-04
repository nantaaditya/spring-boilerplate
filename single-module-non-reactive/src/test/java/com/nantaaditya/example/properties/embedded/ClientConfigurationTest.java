package com.nantaaditya.example.properties.embedded;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClientConfigurationTest {

  @Test
  void isUseProxy_nullProxy_returnsFalse() {
    ClientConfiguration config = new ClientConfiguration(
        "http://example.com", null, null, null, true, false);

    assertFalse(config.isUseProxy());
  }

  @Test
  void isUseProxy_blankProxyHost_returnsFalse() {
    ClientProxyConfiguration proxy = new ClientProxyConfiguration("", 8080);
    ClientConfiguration config = new ClientConfiguration(
        "http://example.com", null, proxy, null, true, false);

    assertFalse(config.isUseProxy());
  }

  @Test
  void isUseProxy_negativePort_returnsFalse() {
    ClientProxyConfiguration proxy = new ClientProxyConfiguration("proxy.example.com", -1);
    ClientConfiguration config = new ClientConfiguration(
        "http://example.com", null, proxy, null, true, false);

    assertFalse(config.isUseProxy());
  }

  @Test
  void isUseProxy_validProxy_returnsTrue() {
    ClientProxyConfiguration proxy = new ClientProxyConfiguration("proxy.example.com", 3128);
    ClientConfiguration config = new ClientConfiguration(
        "http://example.com", null, proxy, null, true, false);

    assertTrue(config.isUseProxy());
  }

  @Test
  void isUseProxy_zeroPort_returnsTrue() {
    ClientProxyConfiguration proxy = new ClientProxyConfiguration("proxy.example.com", 0);
    ClientConfiguration config = new ClientConfiguration(
        "http://example.com", null, proxy, null, true, false);

    assertTrue(config.isUseProxy());
  }
}
