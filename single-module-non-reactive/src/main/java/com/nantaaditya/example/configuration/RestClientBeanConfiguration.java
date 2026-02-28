package com.nantaaditya.example.configuration;

import com.nantaaditya.example.factory.RestClientHelperFactory;
import com.nantaaditya.example.interceptor.ClientLogInterceptor;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.properties.ClientProperties;
import com.nantaaditya.example.properties.embedded.ClientConfiguration;
import com.nantaaditya.example.properties.embedded.ClientNetworkConfiguration;
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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Log4j2
@Configuration
@RequiredArgsConstructor
public class RestClientBeanConfiguration {

  private final ClientProperties clientProperties;
  private final ObservationRegistry observationRegistry;
  private final ClientLogInterceptor clientLoggingInterceptor;

  private static final String POSTFIX_BEAN_NAME = "RestClient";
  private static final int DEFAULT_MAX_PER_ROUTE = 50;
  private static final int DEFAULT_MAX_TOTAL = 100;

  @Bean
  public RestClientHelperFactory restClientHelperFactory(RestTemplateBuilder builder) {
    Map<String, RestTemplate> restClients = new HashMap<>();

    RestClientHelperFactory factory = new RestClientHelperFactory();
    if (clientProperties.configurations() == null || clientProperties.configurations().isEmpty()) {
      log.warn(AppLogMessage.message("#Client - no bean defined"));
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
    log.debug(AppLogMessage.message("#Client - [{}] created", clientProperties.getBeanNames(POSTFIX_BEAN_NAME)));
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
        restTemplate.setInterceptors(List.of(clientLoggingInterceptor));
      }

      restTemplate.setObservationRegistry(observationRegistry);
      return restTemplate;
    } catch (Exception e) {
      log.error(AppLogMessage.message("#Client - error while creating rest client").error(e));
      return null;
    }
  }

  private HttpClientBuilder getHttpClientBuilder(ClientConfiguration clientConfiguration)
      throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    HttpClientBuilder httpClient = HttpClientBuilder.create();

    ClientPoolingConfiguration poolingConfiguration = clientConfiguration.pooling();
    ClientNetworkConfiguration networkConfiguration = this.clientProperties.networkConfiguration();

    ConnectionConfig connectionConfig = createConnectionConfig(networkConfiguration);
    PoolingHttpClientConnectionManagerBuilder connectionManager = createPoolingConnectionManager(
        poolingConfiguration, connectionConfig);

    if (clientConfiguration.disableSslVerification()) {
      TlsSocketStrategy tlsSocketStrategy = createTlsSocketStrategy();
      connectionManager = connectionManager.setTlsSocketStrategy(tlsSocketStrategy);
    }

    ClientTimeOutConfiguration timeOut = clientConfiguration.timeOut();
    RequestConfig requestConfig = createRequestConfiguration(timeOut);
    httpClient = httpClient
        .setConnectionManager(connectionManager.build())
        .setDefaultRequestConfig(requestConfig);

    if (clientConfiguration.isUseProxy()) {
      ClientProxyConfiguration proxyConfiguration = clientConfiguration.proxy();
      HttpHost proxy = new HttpHost(proxyConfiguration.host(), proxyConfiguration.port());
      httpClient = httpClient.setProxy(proxy);
    }

    return httpClient
        .evictIdleConnections(TimeValue.ofMilliseconds(networkConfiguration.evictIdleConnection()))
        .evictExpiredConnections();
  }

  private RequestConfig createRequestConfiguration(ClientTimeOutConfiguration timeOut) {
    return RequestConfig.custom()
        .setConnectTimeout(Timeout.ofMilliseconds(timeOut.connectTimeOut()))
        .setResponseTimeout(Timeout.ofMilliseconds(timeOut.readTimeOut()))
        .setConnectionRequestTimeout(Timeout.ofMilliseconds(timeOut.connectRequestTimeOut()))
        .build();
  }

  private @NonNull TlsSocketStrategy createTlsSocketStrategy()
      throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
    TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
    SSLContext sslContext = SSLContexts.custom()
        .loadTrustMaterial(null, acceptingTrustStrategy)
        .build();
    TlsSocketStrategy tlsSocketStrategy = new DefaultClientTlsStrategy(
        sslContext, (host, session) -> true);
    return tlsSocketStrategy;
  }

  private @NonNull PoolingHttpClientConnectionManagerBuilder createPoolingConnectionManager(
      ClientPoolingConfiguration poolingConfiguration, ConnectionConfig connectionConfig) {
    return PoolingHttpClientConnectionManagerBuilder.create()
        .setMaxConnTotal(
            havePoolConfiguration(poolingConfiguration, ClientPoolingConfiguration::maxTotal)
                ? poolingConfiguration.maxTotal() : DEFAULT_MAX_TOTAL)
        .setMaxConnPerRoute(
            havePoolConfiguration(poolingConfiguration, ClientPoolingConfiguration::maxPerRoute)
                ? poolingConfiguration.maxPerRoute() : DEFAULT_MAX_PER_ROUTE)
        .setDefaultConnectionConfig(connectionConfig);
  }

  private ConnectionConfig createConnectionConfig(ClientNetworkConfiguration networkConfiguration) {
    return ConnectionConfig.custom()
        .setSocketTimeout(Timeout.ofMilliseconds(networkConfiguration.socketTimeOut()))
        .setTimeToLive(Timeout.ofMilliseconds(networkConfiguration.timeToLive()))
        .setValidateAfterInactivity(
            Timeout.ofMilliseconds(networkConfiguration.validateAfterInactivity()))
        .build();
  }

  private boolean havePoolConfiguration(ClientPoolingConfiguration clientPoolingConfiguration,
      Function<ClientPoolingConfiguration, Integer> applyFunction) {
    return Optional.ofNullable(clientPoolingConfiguration)
        .map(applyFunction)
        .filter(result -> result > 0)
        .isPresent();
  }

}
