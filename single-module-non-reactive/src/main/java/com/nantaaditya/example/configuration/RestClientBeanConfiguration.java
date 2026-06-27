package com.nantaaditya.example.configuration;

import com.nantaaditya.example.factory.RestClientHelperFactory;
import com.nantaaditya.example.interceptor.ClientLogInterceptor;
import com.nantaaditya.example.model.dto.AppLogMessage;
import com.nantaaditya.example.properties.ClientProperties;
import com.nantaaditya.example.properties.embedded.ClientConfiguration;
import com.nantaaditya.example.properties.embedded.ClientNetworkConfiguration;
import com.nantaaditya.example.properties.embedded.ClientPoolingConfiguration;
import com.nantaaditya.example.properties.embedded.ClientTimeOutConfiguration;
import io.micrometer.observation.ObservationRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

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
  public RestClientHelperFactory restClientHelperFactory() {
    Map<String, RestClient> restClients = new HashMap<>();

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
            e -> createRestClient(RestClient.builder(), clientProperties.getClientConfiguration(e.getKey())))
        )
    );
    factory.setRestClients(restClients);
    log.debug(AppLogMessage.message("#Client - [{}] created", clientProperties.getBeanNames(POSTFIX_BEAN_NAME)));
    return factory;
  }

  public RestClient createRestClient(RestClient.Builder builder, ClientConfiguration clientConfiguration) {
    try {
      CloseableHttpClient httpClient = buildApacheHttpClient(clientConfiguration);

      // Wrap the Apache client in Spring's factory
      HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

      // REMOVED: factory.setConnectionRequestTimeout() etc.
      // We let Apache HC5 handle the timeouts internally to avoid double-configuration conflicts.

      RestClient.Builder b = builder
          .requestFactory(factory)
          .baseUrl(clientConfiguration.host());

      if (clientConfiguration.enableLog()) {
        b = b.requestInterceptor(clientLoggingInterceptor);
      }
      b = b.observationRegistry(observationRegistry);
      return b.build();
    } catch (Exception e) {
      log.error(AppLogMessage.message("#Client - error while creating rest client").error(e));
      throw e;
    }
  }

  private CloseableHttpClient buildApacheHttpClient(ClientConfiguration config) {
    try {
      HttpClientBuilder clientBuilder = HttpClientBuilder.create();
      ClientTimeOutConfiguration timeOut = config.timeOut();
      ClientPoolingConfiguration pooling = clientProperties.pooling();
      ClientNetworkConfiguration networkConfiguration = clientProperties.networkConfiguration();

      // 1. Connection Config (Socket Level Timeouts)
      ConnectionConfig connectionConfig = ConnectionConfig.custom()
          .setSocketTimeout(Timeout.ofMilliseconds(networkConfiguration.socketTimeOut()))
          .setTimeToLive(Timeout.ofMilliseconds(networkConfiguration.timeToLive()))
          .setValidateAfterInactivity(Timeout.ofMilliseconds(networkConfiguration.validateAfterInactivity()))
          .build();

      // 2. Connection Manager
      PoolingHttpClientConnectionManagerBuilder managerBuilder = PoolingHttpClientConnectionManagerBuilder.create()
          .setMaxConnTotal(pooling.maxTotal())
          .setMaxConnPerRoute(pooling.maxPerRoute())
          .setDefaultConnectionConfig(connectionConfig);

      // 3. SSL Configuration
      if (config.disableSslVerification()) {
        TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
        SSLContext sslContext = SSLContexts.custom()
            .loadTrustMaterial(null, acceptingTrustStrategy)
            .build();
        TlsSocketStrategy tlsSocketStrategy = new DefaultClientTlsStrategy(
            sslContext,
            HostnameVerificationPolicy.CLIENT,
            NoopHostnameVerifier.INSTANCE
        );

        managerBuilder = managerBuilder.setTlsSocketStrategy(tlsSocketStrategy);
      }

      // 4. Request Config (Connect & Response Timeouts)
      RequestConfig requestConfig = RequestConfig.custom()
          .setConnectTimeout(Timeout.ofMilliseconds(timeOut.connectTimeOut()))
          .setResponseTimeout(Timeout.ofMilliseconds(timeOut.readTimeOut()))
          .setConnectionRequestTimeout(Timeout.ofMilliseconds(timeOut.connectRequestTimeOut()))
          .build();

      clientBuilder
          .setConnectionManager(managerBuilder.build())
          .setDefaultRequestConfig(requestConfig)
          .evictIdleConnections(TimeValue.ofMilliseconds(networkConfiguration.evictIdleConnection()))
          .evictExpiredConnections();

      // 5. Proxy Configuration
      if (config.isUseProxy() && config.proxy() != null) {
        HttpHost proxy = new HttpHost(config.proxy().host(), config.proxy().port());
        clientBuilder.setProxy(proxy);
      }

      return clientBuilder.build();

    } catch (Exception e) {
      // Re-throw as a runtime exception to halt Bean creation and crash startup safely
      throw new IllegalStateException("Failed to assemble Apache HttpClient for configuration: " + config.host(), e);
    }
  }

}
