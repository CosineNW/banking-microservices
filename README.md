# 🏦 Banking Microservices

A modular, observable, and scalable backend for a fictional banking system. It uses Spring Boot microservices with Kafka for messaging and Docker Compose for local orchestration. Observability is provided via OpenTelemetry, Prometheus, and Grafana.

---

## 📦 Services

The repository currently includes the following services:

| Service                | Port (host) | Description                                      |
|------------------------|-------------|--------------------------------------------------|
| `customer-service`     | 8181        | Customer profile CRUD                            |
| `transaction-service`  | 8182        | Customer transactions and events                 |
| `reward-service`       | 8183        | Rewards processing (Kafka consumer + MySQL)      |
| `notification-service` | 8184        | Sends notifications based on Kafka events        |
| `gateway-service`      | 8185        | API Gateway (Spring Cloud Gateway)               |
| `preference-service`   | 8186        | User preferences (Kafka consumer)                |
| `service-registry`     | 8762        | Eureka service registry (container 8761 -> host 8762) |

Infrastructure components:

- Kafka (9092) and Zookeeper (2181)
- MySQL (host 3307)
- OpenTelemetry Collector (host 4319 -> container 4318)
- Prometheus (9090)
- Grafana (3000)

---

## 🚀 Features

- Event-driven communication via Apache Kafka
- Distributed tracing with OpenTelemetry (OTLP)
- Metrics via Prometheus with Grafana dashboards
- Docker Compose for one-command local setup
- Clear separation of concerns across services

---

## 🧰 Tech Stack

- Spring Boot (Java 17)
- Apache Kafka
- MySQL
- Docker & Docker Compose
- Prometheus & Grafana
- OpenTelemetry Collector
- Spring Cloud Gateway + Eureka

---

## 📊 Observability Architecture

Microservices → OpenTelemetry Collector → Prometheus → Grafana

Each service is set up for tracing/metrics. Prometheus scrapes metrics and Grafana visualizes them.

---

## ✅ Prerequisites

- Docker and Docker Compose
- Java 17 (for local builds if you’re not building inside Docker)
- Maven (if you build locally)

---

## 🧪 Quick Start

From the Infra directory:

```bash
cd Infra

# Start the entire stack (in the foreground)
docker compose up --build

# Or start detached
docker compose up --build -d
```

Key endpoints once the stack is up:

- Grafana: http://localhost:3000
- Prometheus: http://localhost:9090
- Eureka Dashboard (service registry): http://localhost:8762
- API Gateway: http://localhost:8185
- Kafka broker: localhost:9092
- MySQL: localhost:3307 (user: root, password: root)

Notes:
- Databases created by the initializer: customerdb, transactiondb, rewarddb.
- The OpenTelemetry Collector is exposed on host port 4319.
- If the OTEL collector config path in docker-compose is machine-specific, adjust the volume mapping in Infra/docker-compose.yml.

---

## 📚 Repository Layout

```
.
├── Infra/
│   ├── docker-compose.yml
│   ├── infra/
│   │   └── prometheus/
│   │       └── prometheus.yml
│   ├── mysql-init/        # Optional init scripts
│   └── otel-config.yml    # OpenTelemetry Collector config
├── customer-service/
├── gateway-service/
├── notification-service/
├── preference-service/
├── reward-service/
├── service-registry/
└── transaction-service/
```

Each service typically contains:
- src: Spring Boot source
- config: Kafka/OTel configuration (if applicable)
- Dockerfile: Containerization

---

## 📝 Project Goals

- Documented and modular microservices
- Validated locally with Docker Compose
- Planned: CI/CD and deployment to AWS/GCP

---

## 🧠 Maintainer

Arun — backend engineer focused on clarity, reliability, and observability.

---

## 🤝 Contributing

Issues and PRs are welcome. Open a discussion if you want to add services or improve the stack.





