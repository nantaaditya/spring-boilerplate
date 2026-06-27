# Spring Boot Single-Module Non-Reactive Boilerplate

An example Spring Boot boilerplate project using a single module, non-reactive stack.

## Prerequisites

- Java 21+
- Spring Boot 4.0.6
- Maven 3.9+
- H2 (embedded, for local/test) or any JPA-compatible database

## Capabilities

- Save request on each endpoint call to `event_log` table
- Endpoint to run schema migration manually
- Endpoint to remove obsolete `event_log`
- Endpoint to remove obsolete `dead_letter_process`
- Endpoint to retry `dead_letter_process`
- Masking sensitive PII data on log
- Response time tracing on each endpoint call on log
- Structured log & segregate log for apps, metrics, response time, and error
- Retryable process based on retry policy with Micrometer observability
- External client auto configuration
- OpenAPI & Swagger

## Project Structure

### src/main

Main Java source code, organized by:

- **API** — REST API endpoints
- **CLIENT** — external client call definitions
- **CONFIGURATION** — bean, helper, external library configuration
- **ENTITY** — JPA entities mapping tables to POJOs
- **FACTORY** — factory classes for creating or retrieving bean implementations
- **HELPER** — shared helpers and utilities
- **INTERCEPTOR** — interceptors applied before/after request or response processing
- **LISTENER** — listeners from external dependencies (e.g. retry lifecycle)
- **MODEL** — constants, enums, request DTOs, response DTOs, observation models
- **PROPERTIES** — custom configuration properties, overridable via environment variables
- **REPOSITORY** — database query operations
- **SERVICE** — main business logic
- **STRATEGY** — pluggable strategy implementations (e.g. custom backoff policies)

### .docker

Dockerfile to create a Docker container image.

### .env

Environment variables for running the Docker container image.

### .script

Scripts to build the jar file, Docker image, and run the Docker image.

---

## Features

### Save request on each endpoint call to event_log table

As long as the request sends these headers — `x-client-id`, `x-request-id`, `x-request-time` — and the endpoint path is not in `apps.log.ignored-path`, it will automatically save an event log entry.

### Endpoint to run schema migration manually

Flyway migration runs on startup by default, loading SQL scripts from `src/resources/db/migration`.

To disable startup migration and run it manually, set `spring.flyway.enabled=false`, then call:
```
POST /internal-api/database
```

### Endpoint to remove obsolete event_log

```
DELETE /internal-api/event_log?days=30
```

### Endpoint to remove obsolete dead_letter_process

```
DELETE /internal-api/dead_letter_process?days=30
```

### Endpoint to retry dead_letter_process

```shell
curl -XPOST -H "Content-type: application/json" \
  -d '{"processType":"type","processName":"name","size":30}' \
  'http://localhost:8080/internal-api/dead_letter_process/_retry'
```

Before using this endpoint, create a bean that extends `AbstractRetryProcessorService`:

```java
@Component
public class ExampleRetryProcessor extends AbstractRetryProcessorService {

  public ExampleRetryProcessor(DeadLetterProcessRepository deadLetterProcessRepository,
      ObjectMapper objectMapper) {
    super(deadLetterProcessRepository, objectMapper);
  }

  @Override
  public String getProcessType() {
    return "type";
  }

  @Override
  public String getProcessName() {
    return "name";
  }

  @Override
  public boolean isEligibleToBeRetried(DeadLetterProcess deadLetterProcess) {
    return true;
  }

  @Override
  public <T> void onSuccess(DeadLetterProcess deadLetterProcess, ResponseEntity<T> response) {
    // do something on success
  }

  @Override
  public void onError(DeadLetterProcess deadLetterProcess, Throwable throwable) {
    // do something on error
  }
}
```

### Masking sensitive PII data on log

This feature masks logs on external client calls using RestClient.
Add the sensitive data key to `apps.log.sensitive-field`. It supports both headers and JSON payload fields.

### Response time tracing on each endpoint call on log

Duration tracing is enabled by default for all endpoints not listed in `apps.log.ignored-trace-log-path`.
To disable, set `apps.log.enable-trace-log=false`.

### Structured log & segregate log for apps, metrics, response time, and error

Structured logging uses Log4j2 with a custom JSON format. All log messages must use `AppLogMessage`:

```java
log.info(AppLogMessage.message("k {} v {}", "key", "value")
    .httpRequest(httpRequest)
    .httpResponse(httpResponse)
    .error(throwable));
```

Message format:
```json
{
  "message": "",
  "http_request": {
    "http_method": "",
    "uri": "",
    "headers": { "key": ["value"] },
    "body": {}
  },
  "http_response": {
    "http_method": "",
    "uri": "",
    "http_code": "",
    "duration": "",
    "headers": { "key": ["value"] },
    "body": {}
  },
  "error": {
    "error_message": "",
    "error_stacktrace": [""]
  },
  "additional_data": {}
}
```

Full log envelope:
```json
{
  "timestamp": "2025-12-07 18:52:13:769",
  "level": "INFO",
  "app_version": "1.0.0",
  "class": "com.nantaaditya.example.api.BaseIntegrationTest",
  "context": {
    "message": "",
    "http_request": { "http_method": "", "uri": "", "headers": {}, "body": {} },
    "http_response": { "http_method": "", "uri": "", "http_code": "", "duration": "", "headers": {}, "body": {} },
    "error": { "error_message": "", "error_stacktrace": [""] },
    "additional_data": {}
  },
  "trace_id": "69356a6d142824fb93930087c5b8343a",
  "span_id": "93930087c5b8343a",
  "request_id": "0nsmg6ms5jxme"
}
```

Log files are segregated:
- App log: `${LOG_PATH}/cms-api.log`
- Metric log: `${LOG_PATH}/metrics.log`
- Trace log: `${LOG_PATH}/trace.log`

Default log format is `JSON`. To switch to plain text, set `LOG_FORMAT=TEXT`.

### Retryable process based on retry policy

#### 1. Configure retry properties

Define `apps.retry.configurations.[retryKey]` with the following properties:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `type` | `BackoffPolicyConstant` | — | Backoff strategy: `EXPONENTIAL`, `EXPONENTIAL_RANDOM`, `FIXED`, `UNIFORM_RANDOM` |
| `initial-interval` | `long` | `0` | Initial delay (ms) from first exception to first retry |
| `multiplier` | `double` | `0` | Delay multiplier between retries — applies to `EXPONENTIAL` and `EXPONENTIAL_RANDOM` only |
| `max-interval` | `long` | `0` | Maximum backoff period (ms) |
| `max-attempt` | `int` | `0` | Maximum number of retry attempts |
| `retryable-exceptions` | `String` | `null` | Comma-separated `ClassName:true` (retry on) / `ClassName:false` (do not retry on). Omit to retry all exceptions. Example: `java.io.IOException:true,java.lang.IllegalArgumentException:false` |

Each configured retry key automatically creates a `RetryTemplate` bean named `{retryKey}RetryTemplate`.

#### 2. Inject and use `RetryHelper`

`RetryHelper` is a Spring `@Component` — inject it rather than calling it statically:

```java
@Component
public class ExampleRetry {

  private final RetryHelper retryHelper;

  public ExampleRetry(RetryHelper retryHelper) {
    this.retryHelper = retryHelper;
  }

  public String print(String request) {
    Function<String, String> action = req -> {
      if (req.equals("Hello World")) return req;
      else throw new IllegalArgumentException("error");
    };
    Function<IllegalArgumentException, String> fallback = Throwable::getMessage;

    // resolves "default" RetryTemplate internally from the registry
    return retryHelper.execute("default", "type", "name", action, fallback, request);
  }
}
```

If you need to supply your own `RetryTemplate` directly (advanced use):

```java
retryHelper.execute(retryTemplate, maxRetry, "type", "name", action, fallback, request);
```

All exhausted retry processes are saved to `dead_letter_process` and can be retried manually via the internal API.

#### 3. Retry Observability (Micrometer)

Every retry execution is automatically instrumented. The `RetryTemplateListener` creates a Micrometer observation (`app.retry`) per retry lifecycle and publishes a counter metric `app.retry.attempts` with the following tags:

| Tag | Cardinality | Description |
|-----|-------------|-------------|
| `retry_name` | Low | The configured retry key name |
| `feature` | Low | Feature label mapped from `RetryFeatureConstant` |
| `exception` | Low | Exception class name on failure; `none` on success |
| `outcome` | Low | `SUCCESS`, `EXHAUSTED`, `TIMEOUT`, `INTERRUPTED`, or `UNKNOWN` |
| `attempts` | Low | Number of attempts made |
| `request_id` | High | Request ID from context — stored in the span only, not emitted as a metric tag |

To associate a retry key with a named feature label (for grouping in Prometheus/Grafana), add an entry to `RetryFeatureConstant`:

```java
public enum RetryFeatureConstant {
  DEFAULT("default"),
  MY_FEATURE("myRetryKey"); // maps retry key "myRetryKey" to feature label "MY_FEATURE"
  ...
}
```

If no entry matches the retry key, the `feature` tag defaults to `DEFAULT`.

### Async auto configuration

Auto-configures a thread pool for asynchronous processing. Define `apps.async.configurations.[asyncName]` and annotate your method with `@Async("asyncNameAsyncTaskExecutor")`.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `core-pool-size` | `int` | `5` | ThreadPoolExecutor core pool size |
| `max-pool-size` | `int` | `10` | ThreadPoolExecutor maximum pool size |
| `queue-capacity` | `int` | `50` | Capacity of the BlockingQueue |
| `keep-alive-seconds` | `int` | `60` | Keep-alive time for idle threads |
| `thread-name-prefix` | `String` | `async-` | Prefix for thread names |

### External client auto configuration

Auto-configures external HTTP clients via `apps.client.configurations.[clientName]`.
Retry integration is automatic when `[clientName]` matches a key in `apps.retry.configurations`.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `apps.client.log-format` | `ClientLogFormat` | — | Log format for client request/response: `HTTP` or `JSON` |
| `apps.client.pooling.max-total` | `int` | — | Total maximum connections across all routes (global pool) |
| `apps.client.pooling.max-per-route` | `int` | — | Maximum connections per route (global pool) |
| `apps.client.configurations.[name].host` | `String` | — | Base URL / host for the client |
| `apps.client.configurations.[name].time-out.connect-time-out` | `int` | — | Max time (ms) to establish a connection |
| `apps.client.configurations.[name].time-out.read-time-out` | `int` | — | Max inactivity period (ms) between received data packets |
| `apps.client.configurations.[name].time-out.connect-request-time-out` | `int` | — | Max time (ms) to obtain a connection from the pool |
| `apps.client.configurations.[name].proxy.host` | `String` | — | Proxy host — leave blank to disable |
| `apps.client.configurations.[name].proxy.port` | `int` | — | Proxy port |
| `apps.client.configurations.[name].credential.username` | `String` | — | Basic auth username |
| `apps.client.configurations.[name].credential.password` | `String` | — | Basic auth password |
| `apps.client.configurations.[name].enable-log` | `boolean` | `false` | Enable request/response logging for this client |
| `apps.client.configurations.[name].disable-ssl-verification` | `boolean` | `false` | Disable SSL certificate verification |

To create an external client, define a component that resolves a `RestSender` by name:

```java
@Component
public class MockClient {

  private final RestSender restSender;

  public MockClient(RestSenderHelper restSenderHelper) {
    this.restSender = restSenderHelper.getRestSender("mock");
  }

  public MockClientResponse getMock() {
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    return restSender.execute(
        HttpMethod.GET,
        "/todos/1",
        new HttpHeaders(headers),
        null,
        new ParameterizedTypeReference<MockClientResponse>() {}
      )
      .getBody();
  }
}
```

### OpenAPI & Swagger

Enabled by default at:

```
http://localhost:${PORT}/${CONTEXT_PATH}/swagger-ui/index.html
```
