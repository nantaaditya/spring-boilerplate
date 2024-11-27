# Boilerplate of Spring Boot project using single module and non-reactive
an example of spring boot boilerplate project using single module and non-reactive

## Base Dependency
this is the list of dependencies on this project:
- spring-boot-starter-web
- spring-boot-starter-actuator
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- spring-boot-starter-test
- spring-boot-configuration-processor
- micrometer-tracing-bridge-brave
- flyway-core
- flyway-database-postgresql
- postgresql
- h2
- tsid-creator
- lombok
- tsid-creator

## Project Structure
this is the project structure of this project

### SRC
main java source code, consist of:
- API
this package for storing REST API endpoint
- CONFIGURATION
this package for storing bean, helper, external library, etc configuration
- ENTITY
this package for storing entity, mapping between table on database and POJO class
- HELPER
this package for storing helper
- INTERCEPTOR
this package for storing interceptor before / after the request / response processed
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

### .DOCKER
Dockerfile to create docker container image

### .ENV
Environment variable to run docker container image

### .SCRIPT
Script to build jar file, docker image, and run docker image.
