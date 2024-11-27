Feature: example API
  this endpoint is used to get response from example API.

  Scenario Outline: retrieving response from GET example endpoint
    Given a request GET example
    And with <clientId> clientId request header
    And with <requestId> requestId request header
    And with <requestTime> requestTime request header
    When I hit GET example endpoint
    Then I receive a <httpCode> http status response
    And I receive a GET response
    And I receive a response headers
    Examples:
      | clientId | requestId | requestTime                   | httpCode |
      | clientId |           |                               | 200      |
      | clientId | requestId |                               | 200      |
      | clientId | requestId | 2023-11-27T15:32:12.345+08:00 | 200      |

  Scenario Outline: retrieving response from POST example endpoint
    Given a request POST example
    And with <clientId> clientId request header
    And with <requestId> requestId request header
    And with <requestTime> requestTime request header
    And with <name> name payload
    And with <age> age payload
    When I hit POST example endpoint
    Then I receive a <httpCode> http status response
    And I receive a POST response
    And I receive a response headers
    And I receive a <responseCode> response code
    And I receive a <data> response body
    And I receive a <violationKey> and <violationError> error response
    Examples:
      | clientId | requestId | requestTime                   | httpCode | name | age | responseCode | data | violationKey | violationError |
      | clientId | requestId | 2023-11-27T15:32:12.345+08:00 | 400      |      | 0   | 900          |      | name         | NotBlank       |
      | clientId | requestId | 2023-11-27T15:32:12.345+08:00 | 400      | name | 0   | 900          |      | age          | BelowThreshold |
      | clientId | requestId | 2023-11-27T15:32:12.345+08:00 | 200      | name | 1   | 000          | name |              |                |



