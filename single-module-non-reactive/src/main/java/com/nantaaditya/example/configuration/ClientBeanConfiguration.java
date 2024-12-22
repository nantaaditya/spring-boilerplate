package com.nantaaditya.example.configuration;

import com.google.gson.Gson;
import com.nantaaditya.example.interceptor.ClientLogInterceptor;
import com.nantaaditya.example.properties.ClientProperties;
import com.nantaaditya.example.properties.LogProperties;
import com.nantaaditya.example.properties.embedded.ClientConfiguration;
import com.nantaaditya.example.properties.embedded.ClientPoolingConfiguration;
import com.nantaaditya.example.properties.embedded.ClientProxyConfiguration;
import com.nantaaditya.example.properties.embedded.ClientTimeOutConfiguration;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ClientBeanConfiguration {

  private final Gson gson;
  private final ClientProperties clientProperties;
  private final LogProperties logProperties;
  private final ObservationRegistry observationRegistry;
  private final GenericApplicationContext applicationContext;

  private static final String POSTFIX_BEAN_NAME = "Client";

  @EventListener(ApplicationReadyEvent.class)
  public void onStart() {
    if (clientProperties.configurations() == null || clientProperties.configurations().isEmpty()) {
      log.warn("#Client - no bean defined");
      return;
    }

    clientProperties.configurations()
        .forEach((key, value) -> applicationContext.registerBean(
            key + POSTFIX_BEAN_NAME,
            RestClient.class,
            () -> createRestClient(clientProperties.getClientConfiguration(key)).build(),
            definition -> definition.setLazyInit(true)
        ));

    log.debug("#Client - bean {} created", clientProperties.getBeanNames(POSTFIX_BEAN_NAME));
  }

  public RestClient.Builder createRestClient(ClientConfiguration clientConfiguration) {
    HttpClientBuilder httpClient = getHttpClientBuilder(clientConfiguration);

    ClientTimeOutConfiguration timeOutConfiguration = clientConfiguration.timeOut();
    HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient.build());
    httpRequestFactory.setConnectionRequestTimeout(timeOutConfiguration.connectRequestTimeOut());
    httpRequestFactory.setConnectTimeout(timeOutConfiguration.connectTimeOut());
    httpRequestFactory.setReadTimeout(timeOutConfiguration.readTimeOut());

    RestTemplate restTemplate = new RestTemplate(httpRequestFactory);
    if (clientConfiguration.enableLog()) {
      restTemplate.setInterceptors(List.of(new ClientLogInterceptor(gson, logProperties)));
    }

    return RestClient
        .builder(restTemplate)
        .observationRegistry(observationRegistry);
  }

  private HttpClientBuilder getHttpClientBuilder(ClientConfiguration clientConfiguration) {
    HttpClientBuilder httpClient = HttpClientBuilder.create();

    ClientPoolingConfiguration poolingConfiguration = clientProperties.pooling();
    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setMaxTotal(poolingConfiguration.maxTotal());
    connectionManager.setDefaultMaxPerRoute(poolingConfiguration.maxPerRoute());

    httpClient = httpClient.setConnectionManager(connectionManager);

    if (clientConfiguration.isUseProxy()) {
      ClientProxyConfiguration proxyConfiguration = clientConfiguration.proxy();
      HttpHost proxy = new HttpHost(proxyConfiguration.host(), proxyConfiguration.port());
      httpClient = httpClient.setProxy(proxy);
    }
    return httpClient;
  }
}
