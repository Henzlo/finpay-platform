# FinPay — Distributed Fintech Platform

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.0-brightgreen?logo=spring)](https://spring.io/projects/spring-cloud)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build](https://github.com/Henzlo/finpay-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/Henzlo/finpay-platform/actions/workflows/ci.yml)

## What is FinPay?

FinPay is a production-oriented distributed fintech platform for **loan management**, **EMI collections**, and **NPA resolution**. It is organised as a Maven multi-module Spring Boot / Spring Cloud system: each capability lives in its own deployable service, with a shared API gateway, service discovery, centralised configuration, and an asynchronous Kafka backbone for cross-service events.

The scaffold is ready to run against local Docker infrastructure (PostgreSQL, MongoDB, Redis, Kafka, Elasticsearch/Kibana, Zipkin) and is designed so domain features — underwriting, repayment, delinquency, fraud screening, reporting — can be layered onto clear service boundaries.

## Architecture Overview

```
  Borrowers   Loan Agents   Admins / Ops   Partner APIs
       \           |              |              /
        \          |              |             /
         v         v              v            v
                 ┌─────────────────────────────┐
                 │   API Gateway  (:8080)      │
                 │   JWT edge check · rate     │
                 │   limit (Redis) · routing   │
                 └──────────────┬──────────────┘
                                │
     ┌──────────────────────────┼──────────────────────────┐
     │                          │                          │
     v                          v                          v
┌─────────────┐  ┌──────────────────────┐  ┌──────────────────────┐
│ auth :8081  │  │ Domain services      │  │ Intelligence         │
│ loan :8082  │  │ payment · collections│  │ credit-scoring       │
│ document    │  │ notification         │  │ fraud-detection      │
│ :8089       │  │ reporting            │  │ analytics · chatbot  │
└──────┬──────┘  └──────────┬───────────┘  └──────────┬───────────┘
       │                    │                         │
       └────────────────────┼─────────────────────────┘
                            │
              ┌─────────────┴─────────────┐
              │     Kafka message bus     │
              │  (topics per domain event)│
              └─────────────┬─────────────┘
                            │
   ┌────────────┬───────────┼───────────┬────────────┐
   v            v           v           v            v
PostgreSQL   MongoDB      Redis    Elasticsearch   Zipkin
 (OLTP)    (docs/events) (cache/   (search / logs) (traces)
                         rate limit)
                            │
              ┌─────────────┴─────────────┐
              │ DevOps / observability    │
              │ Eureka · Config Server    │
              │ Kibana · Kafka UI         │
              │ Docker Compose · CI       │
              └───────────────────────────┘
```

**Control plane:** `eureka-server` (registry) and `config-server` (shared config + optional AMQP bus refresh) sit beside the data path. Clients import config with basic-auth credentials; discovery registration is optional when developing a single service in isolation.

## Microservices

| Service | Port | Description | Key Tech |
| --- | ---: | --- | --- |
| `eureka-server` | 8761 | Netflix Eureka service registry | Spring Cloud Netflix Eureka Server |
| `config-server` | 8888 | Central configuration (native classpath or Git) | Spring Cloud Config, Bus AMQP, Security |
| `api-gateway` | 8080 | Edge routing, JWT filter hook, Redis rate limiting | Spring Cloud Gateway, Redis reactive, jjwt |
| `auth-service` | 8081 | Authentication, authorisation, JWT issuing | Spring Security, JPA, Redis, Kafka, springdoc |
| `loan-service` | 8082 | Loan origination, disbursement, repayment schedules | JPA, OpenFeign, Kafka, springdoc |
| `payment-service` | 8083 | Payment capture, settlement, ledger postings | JPA, Redis (idempotency), Kafka, OpenFeign |
| `notification-service` | 8084 | Email / SMS / push dispatch from domain events | MongoDB, Mail, Kafka |
| `analytics-service` | 8085 | Event aggregation and portfolio metrics | MongoDB, Redis cache, Kafka |
| `chatbot-service` | 8086 | Customer support assistant | Spring AI OpenAI, WebFlux, Redis, Feign |
| `credit-scoring-service` | 8087 | Bureau integration and internal risk scoring | JPA, Redis cache, OpenFeign |
| `collections-service` | 8088 | Delinquency tracking, dunning, recovery workflows | JPA, Kafka, OpenFeign, scheduling |
| `document-service` | 8089 | KYC document upload and GridFS storage | MongoDB GridFS, Kafka, Security |
| `fraud-detection-service` | 8090 | Real-time screening and velocity rules | MongoDB, Redis, Kafka |
| `reporting-service` | 8091 | Regulatory and operational report generation | JPA, MongoDB, OpenFeign, scheduling |

## Tech Stack

| Category | Technologies |
| --- | --- |
| Backend | Java 17, Spring Boot 3.2.0, Spring Cloud 2023.0.0, Spring MVC / WebFlux, OpenFeign, MapStruct, Lombok, springdoc-openapi |
| Databases | PostgreSQL 15 (OLTP), MongoDB 7 (documents / GridFS), Redis 7 (cache, sessions, rate limits, idempotency keys) |
| Messaging | Apache Kafka (Confluent 7.5), Zookeeper, Spring Kafka, Spring Cloud Bus AMQP (config refresh) |
| AI/ML | Spring AI 0.8.1 (`spring-ai-openai-spring-boot-starter`) for chatbot completions |
| DevOps | Docker Compose, Makefile shortcuts, GitHub Actions CI, multi-module Maven reactor |
| Security | Spring Security, JJWT 0.11.5, config-server basic auth, gateway public-path allowlist |
| Testing | Spring Boot Test, JUnit (Surefire), planned JaCoCo coverage in CI |
| Monitoring | Spring Boot Actuator, Zipkin, Elasticsearch 8.11 + Kibana, Kafka UI, Micrometer-oriented tracing path (Sleuth is not used on Boot 3.2) |

## Infrastructure Ports

Docker Compose services (`make infra-up`):

| Service | Port | URL |
| --- | ---: | --- |
| PostgreSQL | 5432 | `jdbc:postgresql://localhost:5432/finpay_db` |
| MongoDB | 27017 | `mongodb://localhost:27017` |
| Redis | 6379 | `redis://localhost:6379` |
| Zookeeper | 2181 | `localhost:2181` |
| Kafka (host clients) | 9092 | `localhost:9092` |
| Elasticsearch | 9200 | http://localhost:9200 |
| Kibana | 5601 | http://localhost:5601 |
| Zipkin | 9411 | http://localhost:9411 |
| Kafka UI | 8090 | http://localhost:8090 |

> **Port note:** Kafka UI binds host **8090**, the same default port as `fraud-detection-service`. Stop one or remap the compose mapping if both need to run together.

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+
- Docker Desktop (daemon running)
- Git

### Quick Start

```bash
# 1. Clone
git clone https://github.com/Henzlo/finpay-platform.git
cd finpay-platform

# 2. Local secrets / compose overrides
cp .env.example .env
# Edit .env — at least OPENAI_API_KEY (chatbot), JWT_SECRET, and passwords

# 3. Start infrastructure
make infra-up

# 4. Wait until healthchecks are green
make ps
# Optional: make logs

# 5. Build all modules
mvn clean package -DskipTests

# 6. Start the control plane first
java -jar eureka-server/target/eureka-server-1.0.0-SNAPSHOT.jar
java -jar config-server/target/config-server-1.0.0-SNAPSHOT.jar

# 7. Start remaining services (any order after Eureka + Config are up)
java -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar
java -jar auth-service/target/auth-service-1.0.0-SNAPSHOT.jar
# …repeat for each module under */target/*-1.0.0-SNAPSHOT.jar
```

To work on a single service without discovery/config:

```bash
java -jar loan-service/target/loan-service-1.0.0-SNAPSHOT.jar \
  --eureka.client.enabled=false \
  --spring.cloud.config.enabled=false
```

Point Spring modules at the Compose stack with env vars such as `POSTGRES_USER=finpay_user`, `POSTGRES_PASSWORD=finpay_pass` (module defaults still use `finpay`/`finpay` unless overridden).

### Environment Variables

| Variable | Description |
| --- | --- |
| `POSTGRES_PASSWORD` | Password for Compose Postgres user `finpay_user` |
| `REDIS_PASSWORD` | Optional Redis password (empty for local Compose Redis) |
| `OPENAI_API_KEY` | Required by `chatbot-service`; Spring AI fails startup if unset |
| `CONFIG_SERVER_PASSWORD` | Basic-auth password for config-server (clients use the same value) |
| `JWT_SECRET` | Shared HMAC secret for JWT issue/validate (use a real 256-bit secret outside local) |
| `MONGO_URI` | Example Mongo connection string for local development |
| `POSTGRES_HOST` / `POSTGRES_PORT` / `POSTGRES_USER` | Optional JDBC host overrides for Spring modules |
| `REDIS_HOST` / `REDIS_PORT` | Optional Redis overrides |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers for Spring Kafka clients (default `localhost:9092`) |
| `MONGODB_URI` | Per-service Mongo URI override |
| `EUREKA_URI` | Eureka default zone (default `http://localhost:8761/eureka`) |
| `CONFIG_SERVER_URI` | Config server base URL (default `http://localhost:8888`) |
| `CONFIG_SERVER_USER` | Config server basic-auth username (default `finpay`) |

## Key Design Patterns

| Pattern | Role in FinPay |
| --- | --- |
| **SAGA** | Coordinates multi-service loan / payment / collection workflows with compensating actions on failure |
| **Outbox** | Persists domain events with business data so Kafka publishes are reliable and ordered with the DB commit |
| **CQRS** | Separates write models (loan/payment OLTP) from read/aggregation paths (analytics, reporting) |
| **Circuit Breaker (Resilience4J)** | Isolates Feign calls (credit scoring, fraud, partner APIs) when downstreams degrade |
| **Idempotency** | Redis-backed keys on payment (and similar) APIs so client retries do not double-post |
| **Rate Limiting** | Gateway `RequestRateLimiter` backed by Redis protects edge traffic |
| **Bulkhead** | Thread/connection pools per dependency so one slow service cannot exhaust shared capacity |

> Patterns above describe the intended architecture. Several (SAGA, outbox, Resilience4J bulkheads) are design targets for domain work on top of this scaffold.

## API Documentation

After a servlet-based service is running, OpenAPI UI is at:

```text
http://localhost:{port}/swagger-ui.html
```

Examples: auth `http://localhost:8081/swagger-ui.html`, loan `http://localhost:8082/swagger-ui.html`.  
`eureka-server`, `config-server`, and the reactive `api-gateway` do not expose springdoc UI by default.

## Known Requirements

- **`OPENAI_API_KEY`** must be set for `chatbot-service` or the context fails to start.
- **Redis** must be reachable for `api-gateway` rate limiting (and for several services’ caches / idempotency).
- **Flyway or Liquibase** is recommended before shared or production environments; JPA modules currently use `ddl-auto: update` for local scaffolding only.
- Replace demo **`JWT_SECRET`** and config-server credentials before any non-local deployment.
- Spring Cloud **Sleuth** is not on the classpath (incompatible with Boot 3.2); use Micrometer Tracing + Zipkin for distributed traces.
- Spring AI is pinned to **0.8.1** (Boot 3.2–compatible) from the Spring milestone repository.

## Project Structure

```text
finpay-platform/
├── pom.xml                          # Parent BOM / dependencyManagement
├── README.md
├── Makefile
├── .env.example
├── .gitignore
├── docker-compose.yml
├── docker-compose.override.yml
├── docker/
│   ├── postgres/init/               # Per-service Postgres DBs on first boot
│   └── mongo/init/                  # Per-service Mongo DBs on first boot
├── .github/
│   └── workflows/
│       └── ci.yml
├── eureka-server/
├── config-server/
│   └── src/main/resources/config/   # Native config served to clients
├── api-gateway/
├── auth-service/
├── loan-service/
├── payment-service/
├── notification-service/
├── analytics-service/
├── chatbot-service/
├── credit-scoring-service/
├── collections-service/
├── document-service/
├── fraud-detection-service/
└── reporting-service/
```

Each module follows:

```text
<module>/
├── pom.xml
└── src/main/
    ├── java/com/finpay/<name>/…Application.java
    └── resources/application.yml
```

## License

MIT — see repository license when published.
