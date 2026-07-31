# FinPay Platform

Maven multi-module Spring Boot scaffold for the FinPay fintech microservices platform.

- Group ID `com.finpay`, parent artifact `finpay-platform`, version `1.0.0-SNAPSHOT`
- Java 17, Spring Boot 3.2.0, Spring Cloud 2023.0.0
- 14 modules, each producing an executable Spring Boot jar

## Modules

| Module | Port | Package | Responsibility |
| --- | --- | --- | --- |
| `eureka-server` | 8761 | `com.finpay.eureka` | Service registry |
| `config-server` | 8888 | `com.finpay.config` | Centralised configuration |
| `api-gateway` | 8080 | `com.finpay.gateway` | Edge routing, rate limiting |
| `auth-service` | 8081 | `com.finpay.auth` | Authentication, JWT issuing |
| `loan-service` | 8082 | `com.finpay.loan` | Loan origination and schedules |
| `payment-service` | 8083 | `com.finpay.payment` | Payments and settlement |
| `notification-service` | 8084 | `com.finpay.notification` | Email, SMS, push dispatch |
| `analytics-service` | 8085 | `com.finpay.analytics` | Event roll-ups and metrics |
| `chatbot-service` | 8086 | `com.finpay.chatbot` | Spring AI support assistant |
| `credit-scoring-service` | 8087 | `com.finpay.creditscoring` | Bureau pulls, risk scoring |
| `collections-service` | 8088 | `com.finpay.collections` | Delinquency and recovery |
| `document-service` | 8089 | `com.finpay.document` | KYC document storage |
| `fraud-detection-service` | 8090 | `com.finpay.frauddetection` | Transaction screening |
| `reporting-service` | 8091 | `com.finpay.reporting` | Regulatory reports |

## Build

```bash
mvn clean package
```

Each module's jar is at `<module>/target/<module>-1.0.0-SNAPSHOT.jar`.

## Run

Start `eureka-server`, then `config-server`, then the rest in any order:

```bash
java -jar eureka-server/target/eureka-server-1.0.0-SNAPSHOT.jar
java -jar config-server/target/config-server-1.0.0-SNAPSHOT.jar
java -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar
```

To skip discovery and config while working on a single service:

```bash
java -jar loan-service/target/loan-service-1.0.0-SNAPSHOT.jar \
  --eureka.client.enabled=false --spring.cloud.config.enabled=false
```

## Required infrastructure

Every value below is environment-overridable; the defaults assume everything runs
on localhost. Services do not start without the backing store they use:

| Component | Used by | Default |
| --- | --- | --- |
| PostgreSQL | auth, loan, payment, credit-scoring, collections, reporting | `localhost:5432`, user/password `finpay` |
| MongoDB | notification, analytics, document, fraud-detection, reporting | `localhost:27017` |
| Redis | api-gateway, auth, payment, analytics, chatbot, credit-scoring, fraud-detection | `localhost:6379` |
| Kafka | auth, loan, payment, notification, analytics, collections, document, fraud-detection | `localhost:9092` |
| RabbitMQ | config-server (config bus) | `localhost:5672` |
| SMTP | notification | `localhost:1025` |

Databases are created per service (`finpay_auth`, `finpay_loans`, ...). JPA modules
ship with `ddl-auto: update` for convenience; replace this with Flyway or Liquibase
migrations and set `validate` before any shared environment.

## Configuration notes

- `config-server` is protected with basic auth (`CONFIG_SERVER_USER` /
  `CONFIG_SERVER_PASSWORD`, both defaulting to `finpay`). Clients send these via
  `spring.cloud.config.username` / `password`. Because the import is marked
  `optional:`, a credential mismatch only logs a warning and the service silently
  keeps its bundled defaults, so check for `Located environment` in the log.
- Shared defaults served to all services live in
  `config-server/src/main/resources/config/application.yml`. Switch the config
  server to the `git` profile to source them from `CONFIG_REPO_URI` instead.
- `JWT_SECRET` defaults to a throwaway development key. Replace it everywhere
  before any deployment.
- `chatbot-service` requires `OPENAI_API_KEY`; Spring AI's auto-configuration
  fails startup when it is empty.
- `api-gateway` applies `RequestRateLimiter` as a default filter, so Redis must be
  reachable for any request to be routed.

## Dependency notes

- Spring Boot and Spring Cloud starters take their versions from
  `spring-boot-starter-parent` and the imported `spring-cloud-dependencies` BOM.
  Libraries outside those BOMs (jjwt 0.11.5, MapStruct, springdoc, Spring AI) are
  pinned in the parent's `dependencyManagement`.
- **Spring AI** is pinned to `0.8.1`, the release built against Spring Boot 3.2.x,
  and resolves from `repo.spring.io/milestone`. The versions on Maven Central
  (`1.0.0-M5`/`M6`) target Spring Boot 3.4 and would mix Spring Framework
  versions on this stack.
- **Spring Cloud Sleuth** is pinned in `dependencyManagement` for reference only
  and is intentionally not on any module's classpath. It was superseded by
  Micrometer Tracing in Spring Cloud 2022.x; its last release (3.1.11) targets
  Spring Boot 2.7 and is not compatible here. Use
  `micrometer-tracing-bridge-brave` for distributed tracing.
- `api-gateway` deliberately excludes `spring-boot-starter-security`: on WebFlux it
  would lock every route behind a generated password until a custom
  `SecurityWebFilterChain` exists. Edge token validation belongs in a
  `GlobalFilter` using the jjwt dependencies already present.
- Lombok and MapStruct annotation processing is configured once in the parent's
  `maven-compiler-plugin` `annotationProcessorPaths`, including
  `lombok-mapstruct-binding`. MapStruct generates Spring components by default.

## Layout

Each module follows the same structure; only the main class and `application.yml`
are scaffolded, so add `controller`, `service`, `repository`, `domain`, `dto`,
`mapper` and `config` packages as features land.

```
<module>/
  pom.xml
  src/main/java/com/finpay/<name>/<Name>Application.java
  src/main/resources/application.yml
```
