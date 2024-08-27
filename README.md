# Data Intake RAG Service

This repository contains the Data Intake Service. This service allows for the intake of data from 
clients. The data is provided via Cloud Events, and the payload is stored in a database.

## Environment Variables
### Security Configuration
These values should be set to the values of the Spring authorization server.
* SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI - The URI of the authorization server that issued the JWT token.
* SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI - The URI of the authorization server's JWK set.

### Spring Configuration
The Data Intake Service uses Spring Boot and Spring Data JPA. Two database connections are used to store data. The primary 
database is used to store the data payload, and the keystore database is used to store encryption keys. Two additional 
database connections are required for Flyway migrations. The flyway database users needs to have DDL permissions. While 
the application database user should only have DML permissions (for increased database security).

For the deployment environment, the following environment variables should be set:
* `ORG_JEMBERAI_DATASOURCE_PRIMARY_URL` - The URL of the database
* `ORG_JEMBERAI_DATASOURCE_PRIMARY_USERNAME` - The username for the database
* `ORG_JEMBERAI_DATASOURCE_PRIMARY_PASSWORD` - The password for the `dataintakeservice` database user
* `ORG_JEMBERAI_DATASOURCE_PRIMARY_DRIVER-CLASS-NAME` = `org.postgresql.Driver`
* `ORG_JEMBERAI_JPA_PRIMARY_HIBERNATE_DDL-AUTO` = `validate`
* `SPRING_FLYWAY_URL` - The URL of the database
* `SPRING_FLYWAY_USER` - The username for the database
* `SPRING_FLYWAY_PASSWORD` - The password for the `dataintakeowner` database user
* `ORG_JEMBERAI_DATASOURCE_KEYSTORE_URL` - The URL of the keystore database
* `ORG_JEMBERAI_DATASOURCE_KEYSTORE_USERNAME` - The username for the database
* `ORG_JEMBERAI_DATASOURCE_KEYSTORE_PASSWORD` - The password for the `dataintakeservice` database user
* `ORG_JEMBERAI_DATASOURCE_KEYSTORE_DRIVER-CLASS-NAME` = `org.postgresql.Driver`
* `ORG_JEMBERAI_JPA_KEYSTORE_HIBERNATE_DDL-AUTO` = `validate`
* `ORG_JEMBERAI_DATASOURCE_KEYSTORE-FLYWAY_URL` - The URL of the keystore database
* `ORG_JEMBERAI_DATASOURCE_KEYSTORE-FLYWAY_USERNAME` - The username for the database
* `ORG_JEMBERAI_DATASOURCE_KEYSTORE-FLYWAY_PASSWORD` - The password for the `dataintakeservice` database user
* `ORG_JEMBERAI_DATASOURCE_KEYSTORE-FLYWAY_DRIVER-CLASS-NAME` = `org.postgresql.Driver`
* `OPENAI_API_KEY` - The API key for the OpenAI API
* `SPRING_AI_VECTORSTORE_MILVUS_CLIENT_HOST` - The host of the Milvus server
* `SPRING_AI_VECTORSTORE_MILVUS_CLIENT_PORT` - The port of the Milvus server
* `SPRING_AI_VECTORSTORE_MILVUS_CLIENT_USERNAME` - The username for the Milvus server
* `SPRING_AI_VECTORSTORE_MILVUS_CLIENT_PASSWORD` - The password for the Milvus server

If the default port of 8080 needs to be changed use:
* `SERVER_PORT` - The port to use for the service.

#### Spring Profiles
* `dev` - Development profile for dev cluster
* `json-logs` - Enables JSON log output to console

#### Development Environment Configuration
For the development environment, the following environment variables should be set:
* `SPRING_PROFILES_ACTIVE` = `dev`
* `SPRING_DATASOURCE_PASSWORD` - The password for the `dataintakeservice` database user
* `SPRING_FLYWAY_PASSWORD` - The password for the `dataintakeowner` database user

**Note:** The `SPRING_DATASOURCE_URL`, `SPRING_FLYWAY_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_FLYWAY_USER` are
being set in the `application-dev.properties` file. This is sourced in with the active profile of `dev`.

### OpenShift / K8s Configuration
This application has been configured with Spring Boot Actuator. This provides readiness and liveness probes for OpenShift.

This application will start on port 8080. To override the port, set the SERVER_PORT environment variable to desired port.

#### Example Configuration Snippet
```yaml
        readinessProbe:
          httpGet:
            port: 8080 # Set to server port
            path: /actuator/health/readiness
        livenessProbe:
          httpGet:
            port: 8080
            path: /actuator/health/liveness
```

