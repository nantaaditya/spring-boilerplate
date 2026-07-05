# Spring Boot Single-Module Non-Reactive Boilerplate

## Overview

An example Spring Boot boilerplate project using a single module, non-reactive (Servlet MVC) stack. It bundles the
plumbing that most services need on day one — structured/segregated logging, request auditing, retry with
dead-letter replay, async task execution, external HTTP client configuration, and Micrometer observability — so new
services can focus on business logic instead of re-solving these cross-cutting concerns.

Key capabilities:

- Save request on each endpoint call to `event_log` table
- Endpoint to run schema migration manually
- Endpoint to remove obsolete `event_log`
- Endpoint to remove obsolete `dead_letter_process`
- Endpoint to batch retry `dead_letter_process` by processType/processName
- Endpoint to retry a single `dead_letter_process` record by id
- Masking sensitive PII data on log
- Response time tracing on each endpoint call on log
- Structured log & segregate log for apps, metrics, response time, and error
- Retryable process based on retry policy with Micrometer observability
- SPI to notify external listeners when a retry is exhausted or a dead-letter replay succeeds
- External client auto configuration
- OpenAPI & Swagger

## Architecture

### Request lifecycle

```
Client
  │
  ▼
HeaderFilter            — reads x-client-id/x-request-id/x-request-time, opens the root Micrometer Observation,
  │                        wraps the request body for logging (CacheBodyRequest)
  ▼
DispatcherServlet
  │
  ▼
Controller               — API / internal-api endpoints (thin, delegate to services)
  │
  ▼
Service layer             — business logic, orchestrates repositories, retry helper, async tasks
  │
  ▼
Repository (Spring Data JPA) ── PostgreSQL / H2
```

Errors thrown from any layer are caught centrally by `ApiExceptionHandler` (`@RestControllerAdvice`), which maps
known exception types to a `ResponseCode`, decorates the current Observation with the failure, and returns a
consistent `Response<T>` envelope.

### Cross-cutting concerns

- **Observability** — `ObservationWrapper`/`ObservationHelper` attach a Micrometer `Observation` to the request
  attributes so it can be decorated (success/error) and propagated to async threads (`ObservationWrapper.wrap`).
  `AppObservationListener` logs the lifecycle of observations matching `ObservationConstant` names.
- **Structured logging** — Log4j2 with a custom JSON layout (`JsonLogLayout`/`TextLogLayout`), segregated into app,
  metric, and trace log files. All log statements go through `AppLogMessage` for a consistent envelope.
- **Client logging** — `ClientLogInterceptor` logs outbound `RestClient` calls (request/response, masked sensitive
  fields, duration) and decorates the Observation with the response status or error.
- **Retry & dead-letter** — `RetryHelper` wraps business calls with a named `RetryTemplate`; exhausted retries and
  rejected async tasks are persisted to `dead_letter_process` for later replay via `AbstractRetryProcessorService`.
  `RetryExhaustionNotifier` notifies registered `RetryOutcomeListener` beans when a retry is exhausted or a replay
  succeeds.
- **Async execution** — named thread pools (`apps.async.configurations.[name]`) with a pluggable rejection strategy
  (`LOG_AND_DROP` or `DEAD_LETTER`).

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

**Batch retry** — retries all `NEW` or `FAILED` records matching the given `processType` and `processName`, up to `size` records:

```shell
curl -XPOST -H "Content-type: application/json" \
  -d '{"processType":"ORDER","processName":"placeOrder","size":30}' \
  'http://localhost:8080/internal-api/dead_letter_process/_retry'
```

**Single-record retry** — retries one specific record by its `id`. Unlike the batch endpoint, this also allows force-retrying records stuck in `RETRYING` status (e.g. after a crashed run). `SUCCESS` and `EXHAUSTED` records are still skipped. Returns 404 if the id does not exist.

```shell
curl -XPOST 'http://localhost:8080/internal-api/dead_letter_process/42/_retry'
```

Records enter the `dead_letter_process` table automatically — either from exhausted retries or from rejected async tasks. See the [Dead letter process lifecycle](#dead-letter-process-lifecycle) section for how both paths work and how to register a replay handler.

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
| `rejected-task-strategy` | `AsyncRejectedStrategy` | `LOG_AND_DROP` | What happens when the queue is full and a task is rejected. `LOG_AND_DROP` logs a warning and discards the task. `DEAD_LETTER` saves the task to the `dead_letter_process` table for manual review and replay. |

Tasks that use `DEAD_LETTER` strategy should implement `DeadLetterCapable` to supply `processType`, `processName`, and `payload` for a replayable record. Tasks that do not implement it produce a minimal audit record (`processType = ASYNC_REJECTED`) that is not replayable via the retry API.

To make an async task replayable, implement both `Runnable` and `DeadLetterCapable`:

```java
public class PlaceOrderTask implements Runnable, DeadLetterCapable {

  private final String orderId;
  private final byte[] serializedPayload; // pre-serialized request bytes

  public PlaceOrderTask(String orderId, byte[] serializedPayload) {
    this.orderId = orderId;
    this.serializedPayload = serializedPayload;
  }

  @Override
  public void run() {
    // business logic
  }

  @Override
  public String getProcessType() {
    return "ORDER";
  }

  @Override
  public String getProcessName() {
    return "placeOrder";
  }

  @Override
  public byte[] getPayload() {
    return serializedPayload;
  }
}
```

When the executor rejects this task, a `dead_letter_process` record is created with `processType = ORDER`, `processName = placeOrder`, and the serialized payload — ready for manual replay via the internal API. See the [Dead letter process lifecycle](#dead-letter-process-lifecycle) section for how to set up the replay handler.

### Dead letter process lifecycle {#dead-letter-process-lifecycle}

Records enter the `dead_letter_process` table from two paths:

#### Path 1 — Exhausted retry

When `RetryHelper.execute()` runs out of attempts, `RetryTemplateListener` automatically saves the failed context to `dead_letter_process`. No extra setup is required — any call to `retryHelper.execute(...)` participates in this automatically.

```
retryHelper.execute("default", "ORDER", "placeOrder", action, fallback, request)
    └── attempt 1 → fails
    └── attempt 2 → fails
    └── attempt 3 → fails (max-attempt reached)
        └── RetryTemplateListener saves to dead_letter_process
            processType  = "ORDER"
            processName  = "placeOrder"
            status       = "NEW"
            payload      = serialized request
```

#### Path 2 — Async executor rejection

When an async thread pool's queue is full and `rejected-task-strategy: DEAD_LETTER` is configured, the rejected task is saved to `dead_letter_process` by `DeadLetterRejectedExecutionHandler`.

```yaml
apps:
  async:
    configurations:
      order:
        queue-capacity: 100
        rejected-task-strategy: DEAD_LETTER
```

```
@Async("orderAsyncTaskExecutor")
public void processOrder(PlaceOrderTask task) { ... }

// queue full → task rejected
//   └── DeadLetterRejectedExecutionHandler.rejectedExecution(task, executor)
//       └── task instanceof DeadLetterCapable → saves full metadata
//           processType  = "ORDER"
//           processName  = "placeOrder"
//           status       = "NEW"
//           payload      = task.getPayload()
```

#### Replaying dead letter records

Once records are in the table (from either path), replay them via the API:

```shell
curl -XPOST -H "Content-type: application/json" \
  -d '{"processType":"ORDER","processName":"placeOrder","size":30}' \
  'http://localhost:8080/internal-api/dead_letter_process/_retry'
```

The retry API routes each record through one of two paths, determined by whether `clientName` was set when the record was created:

| `clientName` | Origin | Replay strategy |
|---|---|---|
| non-null | `RetryHelper` with an HTTP client | Resend the original HTTP request via the named `RestSender` |
| null | `RetryHelper` (non-HTTP) or async rejection | Call `replay()` on the registered `AbstractRetryProcessorService` |

For the API to know how to handle a record, register a bean that extends `AbstractRetryProcessorService` with the matching `processType` and `processName`. All abstract methods below must be implemented:

| Method | Purpose |
|---|---|
| `getProcessType()` | Returns the process type label — must match the dead letter record |
| `getProcessName()` | Returns the process name label — must match the dead letter record |
| `isEligibleToBeRetried(DeadLetterProcess)` | Return `false` to skip this record and mark it `SUCCESS` immediately |
| `isSuccess(T response)` | Inspect the result and return `true` if the operation succeeded |
| `toRetryHistoryResponse(T response)` | Serialize the result to a string for the retry history log |
| `onSuccess(DeadLetterProcess, T response)` | Hook called after a successful retry — update downstream state if needed |
| `onError(DeadLetterProcess, Throwable)` | Hook called after a failed retry — alert, notify, or escalate |
| `replay(DeadLetterProcess)` | _(Override for non-HTTP records)_ Re-execute the original business operation |

**HTTP-backed processor** (records with `clientName` set — no `replay()` override needed):

```java
@Component
public class PlaceOrderRetryProcessor extends AbstractRetryProcessorService {

  public PlaceOrderRetryProcessor(DeadLetterProcessRepository deadLetterProcessRepository,
      ObjectMapper objectMapper) {
    super(deadLetterProcessRepository, objectMapper);
  }

  @Override public String getProcessType() { return "ORDER"; }
  @Override public String getProcessName() { return "placeOrder"; }

  @Override
  public boolean isEligibleToBeRetried(DeadLetterProcess deadLetterProcess) {
    return deadLetterProcess.getPayload() != null;
  }

  @Override
  public <T> boolean isSuccess(T response) {
    if (response instanceof ResponseEntity<?> re) return re.getStatusCode().is2xxSuccessful();
    return false;
  }

  @Override
  public <T> String toRetryHistoryResponse(T response) {
    if (response instanceof ResponseEntity<?> re) return String.valueOf(re.getStatusCode().value());
    return String.valueOf(response);
  }

  @Override
  public <T> void onSuccess(DeadLetterProcess deadLetterProcess, T response) {
    // called after HTTP resend succeeds
  }

  @Override
  public void onError(DeadLetterProcess deadLetterProcess, Throwable throwable) {
    // called after HTTP resend fails
  }
}
```

**Non-HTTP processor** (records without `clientName` — must override `replay()`):

```java
@Component
public class PlaceOrderRetryProcessor extends AbstractRetryProcessorService {

  private final OrderService orderService;

  public PlaceOrderRetryProcessor(DeadLetterProcessRepository deadLetterProcessRepository,
      ObjectMapper objectMapper, OrderService orderService) {
    super(deadLetterProcessRepository, objectMapper);
    this.orderService = orderService;
  }

  @Override public String getProcessType() { return "ORDER"; }
  @Override public String getProcessName() { return "placeOrder"; }

  @Override
  public boolean isEligibleToBeRetried(DeadLetterProcess deadLetterProcess) {
    return deadLetterProcess.getPayload() != null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T replay(DeadLetterProcess deadLetterProcess) {
    try {
      PlaceOrderRequest request = objectMapper.readValue(deadLetterProcess.getPayload(), PlaceOrderRequest.class);
      return (T) orderService.placeOrder(request);
    } catch (Exception e) {
      throw new RuntimeException("failed to replay placeOrder", e);
    }
  }

  @Override
  public <T> boolean isSuccess(T response) {
    return response instanceof OrderResponse;
  }

  @Override
  public <T> String toRetryHistoryResponse(T response) {
    return String.valueOf(response);
  }

  @Override
  public <T> void onSuccess(DeadLetterProcess deadLetterProcess, T response) {
    // called after replay() succeeds
  }

  @Override
  public void onError(DeadLetterProcess deadLetterProcess, Throwable throwable) {
    // called when replay() throws — alert, notify, or escalate
  }
}
```

The `update()` method on `AbstractRetryProcessorService` handles status transitions automatically:
- Success → status `SUCCESS`
- Failure, retries remaining → status `FAILED`
- Failure, max retries reached → status `EXHAUSTED`

### Notification SPI for retry outcomes

Consumers can be notified for exactly two outcomes, regardless of which path produced them — never for an ordinary first-attempt success:

| Outcome | Trigger |
|---|---|
| Retry exhausted | `RetryHelper.execute()` runs out of attempts (Path 1 above), **or** a dead-letter replay (`/dead_letter_process/_retry`) hits `EXHAUSTED` |
| Replay succeeded | A dead-letter replay (`/dead_letter_process/_retry`) hits `SUCCESS` — a *live* `RetryHelper.execute()` success does **not** fire this |

Implement `RetryOutcomeListener` and register it as a Spring bean — both methods are default no-ops, so only override the one you need:

```java
@Component
public class OrderRetryAlerting implements RetryOutcomeListener {

  @Override
  public void onRetryExhausted(RetryOutcomeEvent event) {
    // event.source() is INITIAL_RETRY or DEAD_LETTER_REPLAY
    alertingService.notify("retry exhausted for " + event.processType() + "/" + event.processName());
  }

  @Override
  public void onReplaySuccess(RetryOutcomeEvent event) {
    alertingService.notify("dead-letter replay succeeded for " + event.processType() + "/" + event.processName());
  }
}
```

All `RetryOutcomeListener` beans are auto-collected by `RetryExhaustionNotifier` — no manual wiring needed, and registering zero listeners is a no-op. Listeners are invoked independently: if one throws, it's logged and the remaining listeners still run.

`RetryOutcomeEvent` carries `processType`, `processName`, `idempotencyKey`, `retryCount`, `maxRetry`, `lastError`, `source`, and `occurredAt`. Note that `idempotencyKey` differs by `source()`: the request id for `INITIAL_RETRY`, the dead-letter record's actual idempotency key for `DEAD_LETTER_REPLAY`.

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

---

## API Reference

Full interactive docs are served by Swagger at `/swagger-ui/index.html` (see above). Quick reference:

### Public API

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/example` | Sample endpoint returning a static greeting |
| `POST` | `/api/example` | Sample endpoint echoing `name`/`age` from the request body |
| `GET` | `/api/example/mock` | Sample endpoint calling the `mock` external client |

### Internal API

| Method | Path | Description |
|---|---|---|
| `POST` | `/internal-api/database/migrate` | Runs Flyway migration manually. Only registered when `spring.flyway.enabled=true` |
| `DELETE` | `/internal-api/event_log?days=30` | Removes `event_log` records older than `days` |
| `DELETE` | `/internal-api/dead_letter_process?days=30` | Removes `dead_letter_process` records older than `days` |
| `POST` | `/internal-api/dead_letter_process/_retry` | Batch-retries `NEW`/`FAILED` records matching `processType`/`processName`, up to `size` records |
| `POST` | `/internal-api/dead_letter_process/{id}/_retry` | Force-retries a single record by id (also allows retrying `RETRYING` records stuck from a crashed run) |

All responses use the common `Response<T>` envelope (`error` and `data` are omitted when null):

```json
{
  "response": { "code": "000", "description": "success", "time": "2026-07-04T15:42:00+07:00" },
  "data": {},
  "error": { "violations": { "field": ["must not be blank"] } }
}
```

`response.code` comes from `ResponseCode`: `000` success, `400` bad request, `404` not found, `500` internal error,
`900` invalid parameters.

---

## Configuration

Configuration is layered through `src/main/resources/application.yml`, with every value overridable via environment
variable (see the `${VAR:default}` placeholders in the file). A fully generated, per-variable reference — including
type, default, required/secret flags, and a production go-live checklist — is maintained at
[`docs/ENVIRONMENT_VARIABLES.md`](docs/ENVIRONMENT_VARIABLES.md).

Main configuration groups under the `apps.*` prefix:

| Group | Purpose |
|---|---|
| `apps.log.*` | Enable/disable trace, metric, and API logging; sensitive field masking; log format (`JSON`/`TEXT`) |
| `apps.retry.configurations.[retryKey]` | Named `RetryTemplate` beans — backoff policy, intervals, max attempts, retryable exceptions |
| `apps.async.configurations.[asyncName]` | Named thread pool executors — pool sizing, queue capacity, rejection strategy |
| `apps.client.configurations.[clientName]` | Named external HTTP clients — host, timeouts, proxy, basic auth, SSL verification |
| `apps.client.pooling.*` / `apps.client.network-configuration.*` | Shared HTTP connection pool and socket-level tuning applied to all clients |
| `apps.swagger.host` | Host used to build the OpenAPI server URL |

Spring-native groups worth knowing:

- `spring.flyway.*` — controls automatic migration on startup (see [Database](#database))
- `spring.datasource.*` / `spring.jpa.*` — datasource and Hibernate settings
- `management.*` — Actuator port, exposed endpoints, and tracing sampling/baggage propagation

## Local Development

### Prerequisites

- Java 21+
- Maven 3.9+ (or use the bundled `./mvnw` wrapper)
- PostgreSQL (or point `DB_URL` at any JPA-compatible database — H2 is on the test classpath for tests)

### Run from source

```shell
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080` by default (`SERVER_PORT`), with Actuator on a separate port
`http://localhost:1001` (`ACTUATOR_PORT`). Swagger UI is at `http://localhost:8080/swagger-ui/index.html`.

### Run the test suite

```shell
./mvnw test
```

### Build and run as a container

```shell
.script/build_jar.sh      # mvn install — produces target/*.jar and target/dependency/*.jar
.script/build_docker.sh   # docker build -f .docker/Dockerfile -t example:0.0.1 .
.script/docker_run.sh     # docker run -d -p 8080:8080 --env-file .env/dev.env --name example example:0.0.1
```

`.env/dev.env` holds the environment variables consumed by the container (database connection, log path, etc.) — see
[`docs/ENVIRONMENT_VARIABLES.md`](docs/ENVIRONMENT_VARIABLES.md) for the full list and defaults.

## Database

The project uses Spring Data JPA over PostgreSQL, with schema managed by Flyway migrations in
`src/main/resources/db/migration`. Migrations run automatically on startup (`spring.flyway.enabled=true` by
default); see [Endpoint to run schema migration manually](#endpoint-to-run-schema-migration-manually) to trigger
them on demand instead.

### Tables

| Table | Migration | Purpose |
|---|---|---|
| `event_logs` | `V1__create_event_log_table.sql` | One row per logged endpoint call — client/request id, method, path, response code, payload, and additional data |
| `dead_letter_process` | `V2__create_dead_letter_process_table.sql` | Failed/exhausted retry and rejected async task records, replayable via the retry API |

`dead_letter_process` also has a composite index on `(process_type, process_name, status)` to support the batch
retry query.

### Entities

- `BaseEntity` — shared auditing fields (`createdBy`, `updatedBy`, `createdDate`, `updatedDate`) plus an optimistic
  locking `@Version` column, applied via `@MappedSuperclass` + `AuditingEntityListener`
- `EventLog`, `DeadLetterProcess` — map to the tables above
- IDs are generated with `TsidGenerator`/`TimeSeriesId` (time-sortable IDs), except `dead_letter_process.id`, which
  is a `bigserial`

### Local database

For local development, point `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` at any PostgreSQL instance. H2 is available on the
test classpath and is used for the automated test suite only — it is not wired up as a runtime profile.
