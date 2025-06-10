package com.nantaaditya.example.service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

public interface WebClientService {

  interface Builder<T> {
    WebClientService.Request<T> post(String path);

    WebClientService.Request<T> put(String path);

    WebClientService.Request<T> patch(String path);

    WebClientService.Request<T> get(String path);

    WebClientService.Request<T> delete(String path);

    WebClientService.Request<T> option(String path);

    WebClientService.Request<T> head(String path);

    Mono<T> execute();
  }

  interface Request<T> {
    WebClientService.Request<T> pathParameters(Consumer<Map<String, String>> pathParameters);

    WebClientService.Request<T> queryParameters(Consumer<Map<String, List<String>>> queryParameters);

    WebClientService.Request<T> headers(Consumer<Map<String, List<String>>> headers);

    WebClientService.Request<T> cookies(Consumer<Map<String, List<String>>> cookies);

    WebClientService.Request<T> payload(Object payload);

    WebClientService.Request<T> fallback(Mono<T> fallback);

    WebClientService.Request<T> retry(Retry retry);

    Mono<T> execute();
  }
}
