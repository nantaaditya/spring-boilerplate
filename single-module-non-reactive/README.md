# Boilerplate of Spring Boot project using single module and non-reactive
an example of spring boot boilerplate project using single module and non-reactive.
this module include several capabilities:
- save request on each endpoint call to event_log table
- endpoint to run schema migration manually
- endpoint to remove obsolete event_log
- endpoint to remove obsolete dead_letter_process
- endpoint to retry dead_letter_process
- masking sensitive PII data on log
- response time tracing on each endpoint call on log
- segregate log for apps, metrics, response time, and error
- retryable process based on retry policy
- external client auto configuration
- open-api & swagger
- integration test

## Project Structure
this is the project structure of this project

### src/main
main java source code, consist of:
- API
  this package for storing REST API endpoint
- CLIENT
  this package for create client call to external
- CONFIGURATION
  this package for storing bean, helper, external library, etc configuration
- ENTITY
  this package for storing entity, mapping between table on database and POJO class
- FACTORY
  this package for storing factory to create or store implementation of bean.
- HELPER
  this package for storing helper
- INTERCEPTOR
  this package for storing interceptor before / after the request / response processed
- LISTENER
  this package for storing listener from external dependency
- MODEL
  this package for storing DTO like constant, enum, request DTO, response DTO, and etc
- PROPERTIES
  this package for storing custom configuration properties and overridable on env
- REPOSITORY
  this package for storing query action into database
- SERVICE
  this package for storing main business process
- VALIDATOR
  this package for storing custom validation to validate request

### .docker
Dockerfile to create docker container image

### .env
Environment variable to run docker container image

### .script
Script to build jar file, docker image, and run docker image.

## Feature
### save request on each endpoint call to event_log table
as long as your request send these headers: `x-client-id`, `x-request-id`, `x-request-time`, and your endpoint path not included on `apps.log.ignored-path` props it will automatically save event log .

### endpoint to run schema migration manually
By default, it enables flyway migration on startup time, and it will look up sql script on `src/resources/db/migration` path.
If you want to disable on startup time and want to run it manually, just change `spring.flyway.enabled` props to false.
So you can execute REST Api call on this endpoint `/internal-api/database`.

### endpoint to remove obsolete event_log
If at some point your event_log grow bigger, you can easily remove obsolete event log on this endpoint `/internal-api/event_log?days=30`

### endpoint to remove obsolete dead_letter_process
If at some point your dead_letter_process grow bigger, you can easily remove obsolete event log on this endpoint `/internal-api/internal-api/dead_letter_process?days=30`

### endpoint to retry dead_letter_process
If you want to retry dead_letter_process, you can easily do it on this endpoint 
```shell
curl -XPOST -H "Content-type: application/json" \ 
 -d '{"processType":"type","processName":"name","size":30}' \
 'http://localhost:8080/internal-api/dead_letter_process/_retry'
```

before that you need to create bean that extends `AbstractRetryProcessorService`

```java
@Service
public class ExampleRetryProcessorService extends AbstractRetryProcessorService {

  private DeadLetterProcessRepository deadLetterProcessRepository;

  public ExampleRetryProcessorService(DeadLetterProcessRepository deadLetterProcessRepository) {
    super(deadLetterProcessRepository);
  }

  @Override
  public String getProcessType() {
    return "type";
  };
  
  @Override
  public String getProcessName() {
    return "name";
  }
  
  @Override
  protected void doProcess(DeadLetterProcess deadLetterProcess) {
    // do something
  }
}
```

### masking sensitive PII data on log
This feature masking log on external client call using rest client.
Add the sensitive data key on `apps.log.sensitive-field` props, it supports both headers and json payload.

### response time tracing on each endpoint call on log
By default, it enables duration on trace log as long as your endpoint path not ignored on `apps.log.ignored-trace-log-path`.
If you want to disable just change `apps.log.enable-trace-log` props.

### segregate log for apps, metrics, response time, and error
By default, it will segregate log for apps, metrics, response time, and error.
- for app log: `${LOG_PATH}/cms-api.log`
- for metric log: `${LOG_PATH}/metrics.log`
- for error log: `${LOG_PATH}/error.log`
- for trace log: `${LOG_PATH}/trace.log`

### retryable process based on retry policy
To use retry process, you need to create on configuration properties

- create retry configuration properties, `apps.retry.configurations.[retryKey]`, you need to define several props.

| key name             | type                  | default value | description                                                                                                                |
|----------------------|-----------------------|---------------|----------------------------------------------------------------------------------------------------------------------------|
| type                 | BackoffPolicyConstant | -             | type of retry policy, there are 4 options: EXPONENTIAL, EXPONENTIAL_RANDOM, FIXED, UNIFORM_RANDOM                          |
| initial-interval     | long                  | 0             | initial delay from first exception to retry                                                                                |
| multiplier           | double                | 0             | multiplier delay between each retry, only applicable for EXPONENTIAL, EXPONENTIAL_RANDOM                                   |
| max-interval         | long                  | 0             | maximum backoff period                                                                                                     |
| max-attempt          | int                   | 0             | maximum retry attempt                                                                                                      |
| retryable-exceptions | String                | null          | eligible exceptions to be retried, if not set it will retry all kind of exceptions. format: exception:true,exception:false |

- by default, it will create `RetryTemplate` with name `{retryKey}` + `RetryTemplate`
- use `RetryHelper` class to execute method that need to be retried

```java

public class ExampleRetry {

  private final RetryTemplate retryTemplate;

  public ExampleRetry(RetryTemplateHelper retryTemplateHelper) {
    this.retryTemplate = retryTemplateHelper.getRetryTemplate("default");
  }

  public String print(String request) {
    private final Function<String, String> action = (String request) -> {
      if (request.equals("Hello World")) return request;
      else throw new IllegalArgumentException("error");
    };

    private final Function<IllegalArgumentException, String> fallback = Throwable::getMessage;

    return RetryHelper.execute(retryTemplate, "type", "name", action, fallback, "Error");
  }
}
```

- all exhausted retry process will be stored on dead_letter_process, later you can retry it manually

### async auto configuration
this feature will autoconfigure thread pool for asynchronous process, you just need to define `apps.async.configurations.[asyncName]` props.
and annotated your method with `@Async("asyncNameAsyncTaskExecutor")`.

| key name           | type   | default value | description                                                      |
|--------------------|--------|---------------|------------------------------------------------------------------|
| core-pool-size     | int    | 5             | Set the ThreadPoolExecutor's core pool size                      |
| max-pool-size      | int    | 10            | Set the ThreadPoolExecutor's maximum pool size                   |
| queue-capacity     | int    | 50            | Set the capacity for the ThreadPoolExecutor's BlockingQueue      |
| keep-alive-seconds | int    | 60            | Set the ThreadPoolExecutor's keep-alive seconds                  |
| thread-name-prefix | String | async-        | Specify the prefix to use for the names of newly created threads |


### external client auto configuration
this feature will autoconfigure external client using `apps.client.configurations.[clientName]` props.
it also supports with retry policy, as long as the key between `apps.client.configurations.[clientName]` and `apps.retry.configurations.[retryKey]` is the same.

| key name                                                            | type            | default value | description                                                                                                                                                              |
|---------------------------------------------------------------------|-----------------|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| apps.client.log-format                                              | ClientLogFormat | -             | log format for client request & response, there are two options: HTTP, JSON                                                                                              |
| apps.client.pooling.max-total                                       | int             | -             | This parameter sets the absolute maximum number of connections that can be open in the pool across all routes (all target hosts)                                         |
| apps.client.pooling.max-per-route                                   | int             | -             | This parameter sets the maximum number of connections allowed for a specific route (a specific target host)                                                              |
| apps.client.configurations.[name].host                              | String          | -             | client base url / host                                                                                                                                                   |
| apps.client.configurations.[name].time-out.connect-time-out         | int             | -             | This timeout is the maximum time allowed to establish a connection with the target server                                                                                |
| apps.client.configurations.[name].time-out.read-time-out            | int             | -             | Once a connection is successfully established, this timeout defines the maximum period of inactivity between two consecutive data packets being received from the server |
| apps.client.configurations.[name].time-out.connect-request-time-out | int             | -             | This timeout applies when requesting a connection from the connection manager (the pool of connections maintained by HttpClient)                                         |
| apps.client.configurations.[name].proxy.host                        | String          | -             | if your client need to connect through proxy, leave it blank to disable                                                                                                  |
| apps.client.configurations.[name].proxy.port                        | int             | -             | proxy port                                                                                                                                                               |
| apps.client.configurations.[name].credential.username               | String          | -             | client username credential                                                                                                                                               |
| apps.client.configurations.[name].credential.password               | String          | -             | client password credential                                                                                                                                               |
| apps.client.configurations.[name].enable-log                        | String          | false         | enable log for each request & response                                                                                                                                   |
| apps.client.configurations.[name].disable-ssl-verification          | String          | false         | disable ssl verification                                                                                                                                                 |


to create external client, you need to create a bean like this example:

```java
@Component
public class MockClient {

  private RestSender restSender;

  public MockClient(RestSenderHelper restSenderHelper) {
    this.restSender = restSenderHelper.getRestSender("mock"); // get rest sender by name
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


### open-api & swagger
It will enable open-api and swagger by default on this endpoint `http://localhost:${PORT}/${CONTEXT_PATH}/swagger-ui/index.html`