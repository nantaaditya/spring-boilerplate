package com.nantaaditya.example.properties.embedded;

import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public record ClientConfiguration(
    String host,
    ClientTimeOutConfiguration timeOut,
    ClientProxyConfiguration proxy,
    ClientCredentialConfiguration credential,
    boolean enableLog,
    boolean disableSslVerification
) {

  public boolean isUseProxy() {
    return Optional.ofNullable(proxy)
        .filter(p -> StringUtils.isNotBlank(p.host()) && p.port() >= 0)
        .isPresent();
  }
}
