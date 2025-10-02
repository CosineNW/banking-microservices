# Banking Microservices

A fully modular, observable, and scalable backend architecture for a fictional banking system. Built with Spring Boot microservices, Kafka, Docker Compose, Prometheus, Grafana, and (optional) OpenTelemetry.

For comprehensive documentation, see:
- docs/README.md (index)
- docs/architecture.md
- docs/services.md
- docs/eventing.md
- docs/observability.md
- docs/local-development.md

---

## Microservices

| Service               | Description                                        |
|----------------------|----------------------------------------------------|
| `service-registry`   | Eureka-based service discovery                     |
| `gateway-service`    | Spring Cloud Gateway routing and filters           |
| `customer-service`   | CRUD for customer profiles and reward points       |
| `transaction-service`| Transaction CRUD and event publishing              |
| `reward-service`     | Reward record persistence and query (event consumer) |
| `preference-service` | Manages user notification preferences (event producer) |
| `notification-service`| Consumes events and sends notifications (email)   |

---

## Features

- Kafka-based event-driven communication (producers/consumers)
- Metrics via Micrometer on /actuator/prometheus
- Docker Compose orchestration for local testing
- Prometheus + Grafana for dashboards
- Optional distributed tracing via OpenTelemetry Collector

---

## Tech Stack

- Spring Boot 3.x (Java 17/21)
- Apache Kafka
- Spring Cloud Gateway + Eureka
- JPA (MySQL/H2)
- Prometheus & Grafana
- OpenTelemetry Collector

---

## Observability Architecture

[Client] → [Gateway] → [Microservices]  
[Microservices] → [/actuator/prometheus] → [Prometheus] → [Grafana]  
[Microservices] → [OTLP → OTEL Collector] → [Debug logs (default), add a trace backend as needed]

---

## Running Locally (Compose)

Dockerfiles copy built JARs from target/, so build each service first:

```bash
# From repo root, build JARs (example)
cd customer-service && mvn -q -DskipTests package && cd ..
cd transaction-service && mvn -q -DskipTests package && cd ..
cd reward-service && mvn -q -DskipTests package && cd ..
cd preference-service && mvn -q -DskipTests package && cd ..
cd notification-service && mvn -q -DskipTests package && cd ..
cd service-registry && mvn -q -DskipTests package && cd ..
cd gateway-service && mvn -q -DskipTests package && cd ..
```

Start the stack:

```bash
cd Infra
docker compose up --build
```

Useful URLs:
- Grafana: http://localhost:3000
- Prometheus: http://localhost:9090
- Eureka: http://localhost:8762
- Gateway: http://localhost:8185

---

## Repository Layout

banking-microservices/
├── customer-service/
├── transaction-service/
├── reward-service/
├── notification-service/
├── preference-service/
├── gateway-service/
├── service-registry/
├── Infra/
└── docs/

Each service contains:
- src/: Spring Boot source
- resources/: config (Kafka, Actuator, etc.)
- Dockerfile: containerization

---

## Maintainer
Arun — backend engineer focused on clarity, reliability, and observability.

## Contributing
PRs and issues welcome. Open a discussion to add services or improve the stack.





