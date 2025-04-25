package com.nantaaditya.example.configuration;

import com.google.gson.Gson;
import com.nantaaditya.example.factory.RestClientHelperFactory;
import com.nantaaditya.example.interceptor.ClientLogInterceptor;
import com.nantaaditya.example.properties.ClientProperties;
import com.nantaaditya.example.properties.LogProperties;
import com.nantaaditya.example.properties.embedded.ClientConfiguration;
import com.nantaaditya.example.properties.embedded.ClientPoolingConfiguration;
import com.nantaaditya.example.properties.embedded.ClientProxyConfiguration;
import com.nantaaditya.example.properties.embedded.ClientTimeOutConfiguration;
import io.micrometer.observation.ObservationRegistry;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestClientBeanConfiguration {

  private final Gson gson;
  private final ClientProperties clientProperties;
  private final LogProperties logProperties;
  private final ObservationRegistry observationRegistry;

  private static final String POSTFIX_BEAN_NAME = "RestClient";

  @Bean
  public RestClientHelperFactory restClientHelperFactory(RestTemplateBuilder builder) {
    Map<String, RestTemplate> restClients = new HashMap<>();

    RestClientHelperFactory factory = new RestClientHelperFactory();
    if (clientProperties.configurations() == null || clientProperties.configurations().isEmpty()) {
      log.warn("#Client - no bean defined");
      factory.setRestClients(restClients);
      return factory;
    }

    restClients.putAll(clientProperties.configurations()
        .entrySet()
        .stream()
        .collect(Collectors.toMap(
            e -> e.getKey() + POSTFIX_BEAN_NAME,
            e -> createRestClient(builder, clientProperties.getClientConfiguration(e.getKey())))
        )
    );
    factory.setRestClients(restClients);
    log.debug("#Client - [{}] created", clientProperties.getBeanNames(POSTFIX_BEAN_NAME));
    return factory;
  }

  public RestTemplate createRestClient(RestTemplateBuilder builder, ClientConfiguration clientConfiguration) {
    try {
      HttpClientBuilder httpClient = getHttpClientBuilder(clientConfiguration);

      ClientTimeOutConfiguration timeOutConfiguration = clientConfiguration.timeOut();
      HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory(
          httpClient.build());
      httpRequestFactory.setConnectionRequestTimeout(timeOutConfiguration.connectRequestTimeOut());
      httpRequestFactory.setConnectTimeout(timeOutConfiguration.connectTimeOut());
      httpRequestFactory.setReadTimeout(timeOutConfiguration.readTimeOut());

      RestTemplate restTemplate = builder
          .requestFactory(() -> httpRequestFactory)
          .rootUri(clientConfiguration.host())
          .build();
      if (clientConfiguration.enableLog()) {
        restTemplate.setInterceptors(List.of(new ClientLogInterceptor(gson, logProperties)));
      }

      restTemplate.setObservationRegistry(observationRegistry);
      return restTemplate;
    } catch (Exception e) {
      log.error("#Client - error while creating rest client", e);
      return null;
    }
  }

  private HttpClientBuilder getHttpClientBuilder(ClientConfiguration clientConfiguration)
      throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    HttpClientBuilder httpClient = HttpClientBuilder.create();

    ClientPoolingConfiguration poolingConfiguration = clientProperties.pooling();
    PoolingHttpClientConnectionManagerBuilder connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder.create()
        .setMaxConnTotal(poolingConfiguration.maxTotal())
        .setMaxConnPerRoute(poolingConfiguration.maxPerRoute());

    if (clientConfiguration.disableSslVerification()) {
      TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
      SSLContext sslContext = SSLContexts.custom()
          .loadTrustMaterial(null, acceptingTrustStrategy)
          .build();
      TlsSocketStrategy tlsSocketStrategy = new DefaultClientTlsStrategy(
          sslContext, (host, session) -> true);
      connectionManagerBuilder = connectionManagerBuilder.setTlsSocketStrategy(tlsSocketStrategy);
    }

    httpClient = httpClient.setConnectionManager(connectionManagerBuilder.build());

    if (clientConfiguration.isUseProxy()) {
      ClientProxyConfiguration proxyConfiguration = clientConfiguration.proxy();
      HttpHost proxy = new HttpHost(proxyConfiguration.host(), proxyConfiguration.port());
      httpClient = httpClient.setProxy(proxy);
    }
    return httpClient;
  }
}
