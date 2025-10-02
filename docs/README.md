# Banking Microservices — Comprehensive Documentation

This repository implements a modular banking backend using Spring Boot microservices with event-driven integrations via Apache Kafka, service discovery with Eureka, API routing with Spring Cloud Gateway, and observability powered by Prometheus, Grafana, and (optional) OpenTelemetry.

Use this page as the entry point. Deeper sections:
- Architecture overview: docs/architecture.md
- Service reference: docs/services.md
- Eventing and Kafka topics: docs/eventing.md
- Observability and metrics: docs/observability.md
- Local development and running: docs/local-development.md

## What’s in the repo

Core microservices:
- service-registry (Eureka) — centralized service discovery
- gateway-service — single entry point for clients; routes requests to services
- customer-service — CRUD for customer profiles and reward points
- transaction-service — transaction CRUD and publishing transaction events
- reward-service — persistence and query of computed reward records; consumes reward update events
- preference-service — user notification preferences; publishes preference update events
- notification-service — consumes events and sends notifications (email), exposes a test API

Infrastructure (Infra/ via docker-compose):
- Kafka + Zookeeper
- MySQL (+ bootstrap init to create dbs)
- Prometheus + Grafana
- OpenTelemetry Collector (logs pipeline configured)

## Ports and routing

Container ports (service defaults):
- customer-service: 8081
- transaction-service: 8082
- reward-service: 8083
- notification-service: 8084
- gateway-service: 8085
- preference-service: 8086
- service-registry: 8761
- Prometheus: 9090
- Grafana: 3000
- Kafka broker: 9092

When running with docker-compose (Infra/docker-compose.yml), services are published to host via:
- customer-service: localhost:8181 → container :8081
- transaction-service: localhost:8182 → :8082
- reward-service: localhost:8183 → :8083
- notification-service: localhost:8184 → :8084
- gateway-service: localhost:8185 → :8085
- preference-service: localhost:8186 → :8086
- service-registry: localhost:8762 → :8761
- Prometheus: localhost:9090
- Grafana: localhost:3000
- Kafka: localhost:9092
- OTEL Collector: host port 4319 → container :4318

Gateway routes (current configuration):
- /customer/** → http://localhost:8081 (customer-service)
- /notification/** → http://localhost:8084 (notification-service)

## Key technologies

- Java 17/21 (varies by service) + Spring Boot 3.x
- Spring Cloud Gateway (API gateway)
- Spring Cloud Netflix Eureka (service discovery)
- Apache Kafka (event bus)
- JPA (MySQL/H2 persistence)
- Micrometer + Prometheus (metrics)
- Grafana (dashboards)
- OpenTelemetry Collector (OTLP; logs pipeline enabled)

## Quick start

Prerequisites:
- Docker and Docker Compose
- Java 17 or 21 (to build)
- Maven

Steps:
1) Build each service JAR (needed because Dockerfiles COPY target/*.jar)
   - From repo root:
     - cd customer-service && mvn -q -DskipTests package && cd ..
     - cd transaction-service && mvn -q -DskipTests package && cd ..
     - cd reward-service && mvn -q -DskipTests package && cd ..
     - cd preference-service && mvn -q -DskipTests package && cd ..
     - cd notification-service && mvn -q -DskipTests package && cd ..
     - cd service-registry && mvn -q -DskipTests package && cd ..
     - cd gateway-service && mvn -q -DskipTests package && cd ..

2) Start the stack:
   - cd Infra
   - docker compose up --build

3) Verify:
   - Grafana: http://localhost:3000
   - Prometheus: http://localhost:9090
   - Eureka dashboard: http://localhost:8762
   - Gateway: http://localhost:8185
   - Customer service health: http://localhost:8181/actuator/health

## Example API calls (docker-compose ports)

- Create a customer:
  curl -X POST http://localhost:8181/api/customers/create \
    -H "Content-Type: application/json" \
    -d '{"customerId":"C-1001","name":"Alice","email":"alice@example.com","phone":"1234567890","rewardPoints":0}'

- Create a transaction (emits transaction-events):
  curl -X POST http://localhost:8182/api/transactions/save \
    -H "Content-Type: application/json" \
    -d '{"transactionId":"T-2001","customerId":"C-1001","amount":125.50,"currency":"USD","type":"DEBIT","timestamp":"2024-12-01T12:00:00Z","description":"POS"}'

- Update user preference (publishes user-preference-updated):
  curl -X PUT http://localhost:8186/preferences/save \
    -H "Content-Type: application/json" \
    -d '{"userId":"C-1001","email":true,"sms":false,"push":true}'

- Send a test notification:
  curl -X POST http://localhost:8184/api/notify/send \
    -H "Content-Type: application/json" \
    -d '{"recipient":"alice@example.com","subject":"Hello","body":"Test message"}'

See docs/services.md for full endpoint lists and data models.

## Event topics (high level)

- transaction-events: produced by transaction-service (TransactionEvent payload)
- user-preference-updated: produced by preference-service (PreferenceDTO payload), consumed by notification-service
- customer-reward-updated: consumed by reward-service and notification-service (CustomerRewardUpdatedEvent payload). The producer for this event is planned; see docs/eventing.md.

## Observability

- Every service exposes /actuator/health and /actuator/prometheus.
- Prometheus job config scrapes services at host.docker.internal on their container ports.
- Grafana listens at http://localhost:3000 (configure dashboards to visualize service and Kafka metrics).
- OTEL Collector is set up with an HTTP receiver and a debug logs exporter (no trace backend configured by default).

For details and metrics names, see docs/observability.md.