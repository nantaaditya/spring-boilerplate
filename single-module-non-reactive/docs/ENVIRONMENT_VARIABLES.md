# Environment Variables Reference

> Auto-generated from `src/main/resources/application.yml`.
> Last updated: 2026-06-27

## Legend

| Symbol | Meaning |
|--------|---------|
| 🔒 | Secret — never commit; rotate if exposed |
| ✅ | Required — no default; app won't start without it |

---

## Go-Live Checklist

Before deploying to production, verify:

- [ ] `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` point to the production database
- [ ] `DB_PASSWORD` is rotated from the default placeholder value
- [ ] All `🔒 Secret` variables are set via secrets manager, not `.env` files
- [ ] `JPA_SHOW_SQL` is `false` in production
- [ ] `HIBERNATE_FORMAT_SQL` is `false` in production
- [ ] `ACTUATOR_EXPOSED` is restricted (not `*`) in production
- [ ] `ACTUATOR_SHUTDOWN` is `none` in production
- [ ] `HEALTH_DETAIL` is `never` or `when-authorized` in production
- [ ] `MOCK_CLIENT_HOST` points to the real production endpoint
- [ ] `FLYWAY_ENABLE` is `true` on first deploy; confirm migration succeeded before proceeding

---

## 1. Server

| Variable | Description | Type | Default | Required | Sensitivity | Nonprod Value | Prod Value | Notes |
|----------|-------------|------|---------|----------|-------------|---------------|------------|-------|
| `SERVER_PORT` | HTTP port the embedded server listens on | Integer | `8080` | No | | | | |
| `CONTEXT_PATH` | Servlet context path prefix for all endpoints | String | `` (empty) | No | | | | Leave empty for root path |

---

## 2. Application

| Variable | Description | Type | Default | Required | Sensitivity | Nonprod Value | Prod Value | Notes |
|----------|-------------|------|---------|----------|-------------|---------------|------------|-------|
| `APPLICATION_NAME` | Spring application name used in traces and logs | String | `example-app` | No | | | | |
| `VIRTUAL_THREAD_ENABLED` | Enable Java 21 virtual threads for request handling | Boolean | `true` | No | | | | Requires Java 21+ |
| `APP_VERSION` | Application version reported in logs and metrics | String | `1.0.0` | No | | | | |

---

## 3. Database

| Variable | Description | Type | Default | Required | Sensitivity | Nonprod Value | Prod Value | Notes |
|----------|-------------|------|---------|----------|-------------|---------------|------------|-------|
| `DB_URL` | JDBC connection URL for the primary database | URL | `jdbc:postgresql://localhost:5432/boilerplate` | No | | | | |
| `DB_USERNAME` | Database login username | String | `user` | No | | | | |
| `DB_PASSWORD` | Database login password | String | `password` | No | 🔒 Secret | | | ⚠️ Rotate before prod — default is a plaintext placeholder |
| `DB_DRIVER` | Fully-qualified JDBC driver class name | String | `org.postgresql.Driver` | No | | | | |
| `HIKARI_MIN_POOL` | HikariCP minimum number of idle connections | Integer | `10` | No | | | | |
| `HIKARI_MAX_POOL` | HikariCP maximum connection pool size | Integer | `25` | No | | | | |
| `HIKARI_CONNECTION_TIMEOUT` | Max time (ms) to wait for a connection from the pool | Long (ms) | `5000` | No | | | | |
| `HIKARI_IDLE_TIMEOUT` | Max time (ms) a connection may sit idle in the pool | Long (ms) | `30000` | No | | | | |
| `HIKARI_MAX_LIFETIME` | Max lifetime (ms) of a connection in the pool | Long (ms) | `300000` | No | | | | |

---

## 4. Flyway (Database Migration)

| Variable | Description | Type | Default | Required | Sensitivity | Nonprod Value | Prod Value | Notes |
|----------|-------------|------|---------|----------|-------------|---------------|------------|-------|
| `FLYWAY_ENABLE` | Enable Flyway schema migration on startup | Boolean | `true` | No | | | | Set `false` to run migration manually via `POST /internal-api/database` |
| `FLYWAY_BASELINE_ON_MIGRATE` | Baseline an existing schema before running migrations | Boolean | `true` | No | | | | |
| `FLYWAY_VALIDATE_ON_MIGRATE` | Validate applied migrations against scripts on startup | Boolean | `true` | No | | | | |

---

## 5. JPA / Hibernate

| Variable | Description | Type | Default | Required | Sensitivity | Nonprod Value | Prod Value | Notes |
|----------|-------------|------|---------|----------|-------------|---------------|------------|-------|
| `JPA_SHOW_SQL` | Print SQL statements to the log | Boolean | `true` | No | | | | Set `false` in production to avoid SQL leakage in logs |
| `HIBERNATE_FORMAT_SQL` | Pretty-print SQL statements in the log | Boolean | `true` | No | | | | Set `false` in production |

---

## 6. Logging

| Variable | Description | Type | Default | Required | Sensitivity | Nonprod Value | Prod Value | Notes |
|----------|-------------|------|---------|----------|-------------|---------------|------------|-------|
| `ROOT_LOG_LEVEL` | Root logger level | String | `INFO` | No | | | | Values: `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR` |
| `SPRING_LOG_LEVEL` | Log level for Spring Framework classes | String | `INFO` | No | | | | |
| `HIBERNATE_LOG_LEVEL` | Log level for Hibernate ORM classes | String | `WARN` | No | | | | |
| `ZALANDO_LOG_LEVEL` | Log level for Logbook HTTP request/response logging | String | `TRACE` | No | | | | Set `WARN` in production to reduce log volume |
| `APP_LOG_LEVEL` | Log level for application classes under `com.nantaaditya` | String | `INFO` | No | | | | |
| `LOG_PATH` | File system path where log files are written | String | `.logs/` | No | | | | Ensure the path is writable by the process user |
| `LOG_FORMAT` | Log output format for the application log appender | String | `JSON` | No | | | | Values: `JSON`, `TEXT`. Use `JSON` for structured log aggregators (ELK, Loki) |

---

## 7. Actuator & Observability

| Variable | Description | Type | Default | Required | Sensitivity | Nonprod Value | Prod Value | Notes |
|----------|-------------|------|---------|----------|-------------|---------------|------------|-------|
| `ACTUATOR_PORT` | Port for the actuator management server | Integer | `1001` | No | | | | Keep on a separate port to restrict network access in production |
| `ACTUATOR_SHUTDOWN` | Access level for the shutdown actuator endpoint | String | `none` | No | | | | Values: `none`, `unrestricted`. Keep `none` in production |
| `HEALTH_DETAIL` | Visibility of health check details | String | `always` | No | | | | Set `never` or `when-authorized` in production |
| `ACTUATOR_EXPOSED` | Comma-separated actuator endpoints to expose | String | `*` | No | | | | Restrict to `health,info,prometheus` in production |
| `PROMETHEUS_ENABLED` | Enable Prometheus metrics export | Boolean | `true` | No | | | | |
| `TRACING_SAMPLING_PROBABILITY` | Fraction of requests sampled for distributed tracing | Double | `1.0` | No | | | | Reduce to `0.1`–`0.5` in high-traffic production environments |
| `TRACING_CORRELATION_FIELDS` | Baggage fields included in trace correlation | String | `requestId` | No | | | | Comma-separated |
| `TRACING_LOCAL_FIELDS` | Baggage fields propagated locally within the JVM | String | `requestId` | No | | | | Comma-separated |
| `TRACING_REMOTE_FIELDS` | Baggage fields propagated over the wire to downstream services | String | `requestId` | No | | | | Comma-separated |

---

## 8. Request Logging

| Variable | Description | Type | Default | Required | Sensitivity | Nonprod Value | Prod Value | Notes |
|----------|-------------|------|---------|----------|-------------|---------------|------------|-------|
| `ENABLE_TRACE_LOG` | Enable per-request duration trace logging | Boolean | `true` | No | | | | |
| `ENABLE_METRIC_LOG` | Enable metric log output to the metrics log file | Boolean | `true` | No | | | | |
| `ENABLE_API_LOG` | Enable saving each request to the `event_log` table | Boolean | `true` | No | | | | |
| `IGNORED_TRACE_LOG_PATH` | Path patterns excluded from trace logging | String | `*/swagger-ui,*/swagger-ui/*,*/v3/api-docs,*/v3/api-docs/*` | No | | | | Comma-separated wildcard patterns |
| `SENSITIVE_FIELDS` | Header or JSON field names to mask in logs | String | `x-client-id` | No | | | | Comma-separated; extend with any PII-carrying fields |

---

## 9. Swagger / OpenAPI

| Variable | Description | Type | Default | Required | Sensitivity | Nonprod Value | Prod Value | Notes |
|----------|-------------|------|---------|----------|-------------|---------------|------------|-------|
| `SWAGGER_HOST` | Base URL used for the Swagger UI server entry | URL | `http://localhost` | No | | | | Set to the public hostname in production |

---

## 10. Async Thread Pools

### Default Async Pool (`apps.async.configurations.default`)

| Variable | Description | Type | Default | Required | Sensitivity | Nonprod Value | Prod Value | Notes |
|----------|-------------|------|---------|----------|-------------|---------------|------------|-------|
| `ASYNC_DEFAULT_CORE_POOL_SIZE` | Core thread count for the default async executor | Integer | `5` | No | | | | |
| `ASYNC_DEFAULT_MAX_POOL_SIZE` | Maximum thread count for the default async executor | Integer | `10` | No | | | | |
| `ASYNC_DEFAULT_QUEUE` | Task queue capacity for the default async executor | Integer | `50` | No | | | | |
| `ASYNC_DEFAULT_KEEP_ALIVE` | Idle thread keep-alive time (seconds) for the default async executor | Integer | `60` | No | | | | |
| `ASYNC_DEFAULT_THREAD_NAME` | Thread name prefix for the default async executor | String | `async-` | No | | | | |
| `ASYNC_DEFAULT_REJECTED_STRATEGY` | Behavior when the async queue is full and a task is rejected | String | `LOG_AND_DROP` | No | | | | Values: `LOG_AND_DROP` (log and discard the task), `DEAD_LETTER` (save to `dead_letter_process` for manual replay). Tasks must implement `DeadLetterCapable` for full replay metadata; otherwise a minimal audit record is saved. |

---

## 11. Retry Policy (`apps.retry.configurations.default`)

| Variable | Description | Type | Default | Required | Sensitivity | Nonprod Value | Prod Value | Notes |
|----------|-------------|------|---------|----------|-------------|---------------|------------|-------|
| `DEFAULT_RETRY_TYPE` | Backoff strategy for the default retry configuration | String | `EXPONENTIAL_RANDOM` | No | | | | Values: `EXPONENTIAL`, `EXPONENTIAL_RANDOM`, `FIXED`, `UNIFORM_RANDOM` |
| `DEFAULT_RETRY_INITIAL_INTERVAL` | Initial delay (ms) before the first retry attempt | Long (ms) | `1500` | No | | | | |
| `DEFAULT_RETRY_MULTIPLIER` | Backoff multiplier applied between retry attempts | Double | `2.0` | No | | | | Applies to `EXPONENTIAL` and `EXPONENTIAL_RANDOM` only |
| `DEFAULT_RETRY_MAX_INTERVAL` | Maximum backoff delay (ms) between retries | Long (ms) | `12000` | No | | | | |
| `DEFAULT_RETRY_MAX_ATTEMPT` | Maximum number of retry attempts before exhaustion | Integer | `3` | No | | | | |
| `DEFAULT_RETRY_EXCEPTIONS` | Retryable exception class mappings | String | `java.lang.IllegalArgumentException:true` | No | | | | Comma-separated `ClassName:true/false` pairs — `true` = retry on this exception |

---

## 12. External Clients

### Mock Client (`apps.client.configurations.mock`)

| Variable | Description | Type | Default | Required | Sensitivity | Nonprod Value | Prod Value | Notes |
|----------|-------------|------|---------|----------|-------------|---------------|------------|-------|
| `MOCK_CLIENT_HOST` | Base URL for the mock external client | URL | `https://jsonplaceholder.typicode.com` | No | | | | Replace with the real service URL in production |
| `MOCK_CLIENT_CONNECT_TIMEOUT` | Max time (ms) to establish a connection to the mock client | Long (ms) | `2000` | No | | | | |
| `MOCK_CLIENT_READ_TIMEOUT` | Max inactivity time (ms) between data packets from the mock client | Long (ms) | `2000` | No | | | | |
| `MOCK_CLIENT_CONNECT_REQUEST_TIMEOUT` | Max time (ms) to obtain a connection from the pool for the mock client | Long (ms) | `2000` | No | | | | |

---

## 13. Client Connection Pool (Global)

| Variable | Description | Type | Default | Required | Sensitivity | Nonprod Value | Prod Value | Notes |
|----------|-------------|------|---------|----------|-------------|---------------|------------|-------|
| `CLIENT_MAX_IDLE_CONNECTIONS` | Total maximum connections in the global HTTP client pool across all routes | Integer | `10` | No | | | | Shared across all configured clients |
| `CLIENT_MAX_IDLE_CONNECTION_PER_ROUTE` | Maximum connections per route in the global HTTP client pool | Integer | `5` | No | | | | |
| `SOCKET_TIMEOUT` | Socket-level inactivity timeout (ms) for all HTTP clients | Long (ms) | `10000` | No | | | | |
| `TIME_TO_LIVE` | Maximum lifetime (ms) of a pooled HTTP connection | Long (ms) | `60000` | No | | | | |
| `VALIDATE_AFTER_INACTIVITY` | Time (ms) after which an idle connection is validated before reuse | Long (ms) | `5000` | No | | | | |
| `EVICT_IDLE_CONNECTIONS` | Interval (ms) for background eviction of idle connections from the pool | Long (ms) | `15000` | No | | | | |
