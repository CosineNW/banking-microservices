# 🏦 Banking Microservices

A modular, observable, and scalable backend for a fictional banking system. It uses Spring Boot microservices with Kafka for messaging and Docker Compose for local orchestration. Observability is provided via OpenTelemetry, Prometheus, and Grafana.

---

## Contents

- Overview
- Services and Ports
- Features
- Tech Stack
- Architecture and Observability
- Prerequisites
- Quick Start
- Health, Logs, and Dashboards
- Developer Workflow (build, test, run single service)
- API via Gateway (example requests)
- Repository Layout
- Troubleshooting (FAQ)
- Extending the System
- Contributing
- License

---

## 📦 Services and Ports

The repository includes the following services:

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
- Health endpoints via Spring Boot Actuator

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

## 🏗️ Architecture and Observability

High-level flow:

- Clients → API Gateway → Microservices
- Microservices → Kafka (produce/consume events)
- Microservices → OpenTelemetry Collector (metrics/traces)
- Prometheus scrapes metrics from the collector and/or services
- Grafana dashboards visualize metrics

ASCII overview:

```
           +------------------+
           |      Client      |
           +--------+---------+
                    |
                    v
          +---------+----------+
          |    API Gateway     | 8185
          +----+-----------+---+
               |           |
      +--------+--+   +----+--------+
      | customer  |   | transaction  |
      | 8181      |   | 8182         |
      +-----+-----+   +----+---------+
            |              |
            | Kafka events |
            v              v
      +-----------+   +-----------+
      |  reward   |   | preference|
      |   8183    |   |   8186    |
      +-----+-----+   +-----+-----+
            |                |
            +-------> notification (8184)
                    
 Metrics/Traces:
   services -> OTEL Collector (4318 in container, 4319 host)
   Prometheus (9090) scrapes, Grafana (3000) visualizes
   Eureka registry (8762 host -> 8761 container)
```

Notes:
- Tracing export is configured via the OpenTelemetry Collector. See `Infra/otel-config.yml` for exporters and pipelines.
- Prometheus config is in `Infra/infra/prometheus/prometheus.yml`.

---

## ✅ Prerequisites

- Docker and Docker Compose
- Java 17 (if building locally)
- Maven (if building locally)

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

- Grafana: http://localhost:3000 (default admin/admin on first run)
- Prometheus: http://localhost:9090
- Eureka Dashboard (service registry): http://localhost:8762
- API Gateway: http://localhost:8185
- Kafka broker: localhost:9092
- MySQL: localhost:3307 (user: root, password: root)

Notes:
- Databases created by the initializer: `customerdb`, `transactiondb`, `rewarddb`.
- The OpenTelemetry Collector is exposed on host port 4319.
- Windows users: `Infra/docker-compose.yml` currently mounts `otel-config.yml` with an absolute Windows path. Replace it with a relative path mapping if needed:
  - Current: `D:\ProgramsUser\IntelliJ\workpalces\Banking\Infra\otel-config.yml:/etc/otel/config.yml`
  - Recommended (cross-platform): `./otel-config.yml:/etc/otel/config.yml`

To stop and clean up:

```bash
# Stop containers
docker compose down

# Stop and remove volumes (including databases)
docker compose down -v
```

---

## 🧑‍⚕️ Health, Logs, and Dashboards

- Health checks: each Spring service should expose `/actuator/health`
  - Example: http://localhost:8181/actuator/health
- Logs:
  - Follow all containers: `docker compose logs -f`
  - Specific service: `docker compose logs -f customer-service`
- Grafana:
  - First-time login: admin / admin (you will be prompted to set a new password)
  - Add dashboards and explore metrics from Prometheus data source
- Prometheus:
  - Check targets and scrape status at http://localhost:9090/targets

---

## 👩‍💻 Developer Workflow

Build and test a service locally (outside Docker):

```bash
# Example for customer-service
cd customer-service
./mvnw clean verify

# Run locally
./mvnw spring-boot:run
```

Build an image for a single service:

```bash
# From service directory (example)
cd transaction-service
docker build -t transaction-service:local .
```

Run only infrastructure (Kafka, MySQL, OTel, Prometheus, Grafana) and start one service locally:

```bash
cd Infra
docker compose up -d kafka zookeeper mysql otel-collector prometheus grafana service-registry gateway-service

# Then start a single service locally in its directory
cd ../customer-service
./mvnw spring-boot:run
```

Environment configuration:
- Each service contains `application.yml` (and possibly `application-dev.yml`) for Kafka topics, MySQL connection, and OTEL exporter.
- Values overridden in Compose:
  - MySQL URLs, usernames, passwords
  - `SPRING_KAFKA_BOOTSTRAP_SERVERS` where applicable
  - `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE` for Gateway
  - `OTEL_EXPORTER_OTLP_ENDPOINT` for Gateway

---

## 🌐 API via Gateway

The API Gateway forwards requests to services registered in Eureka. Common patterns:

- Customers:
  - Create: `POST http://localhost:8185/api/customers`
  - Get by ID: `GET http://localhost:8185/api/customers/{id}`
  - List: `GET http://localhost:8185/api/customers`

- Transactions:
  - Create: `POST http://localhost:8185/api/transactions`
  - Get by ID: `GET http://localhost:8185/api/transactions/{id}`
  - List by customer: `GET http://localhost:8185/api/transactions?customerId={id}`

Example request (adjust fields to match your DTOs):

```bash
# Create customer
curl -X POST http://localhost:8185/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jane Doe",
    "email": "jane@example.com"
  }'

# Create transaction
curl -X POST http://localhost:8185/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUSTOMER_ID",
    "amount": 125.50,
    "type": "PURCHASE",
    "description": "Coffee shop"
  }'
```

Kafka events:
- Services publish/consume Kafka events based on actions (e.g., transactions may emit events consumed by rewards or notifications).
- Check the specific service `application.yml` or Kafka config classes for topic names.

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
- `src`: Spring Boot source
- `config`: Kafka/OTel configuration (if applicable)
- `Dockerfile`: Containerization

---

## 🛠️ Troubleshooting (FAQ)

- Ports already in use:
  - Stop processes using conflicting ports (e.g., `lsof -i :9092`) or adjust host port mappings in `Infra/docker-compose.yml`.

- Kafka or Zookeeper fails to start:
  - Ensure Docker has enough memory. Restart Docker if brokers fail repeatedly.
  - Check container logs: `docker compose logs kafka zookeeper -f`.

- MySQL connection issues:
  - Confirm the container is healthy: `docker compose ps`.
  - Verify credentials and host port 3307. Check service `application.yml` JDBC URLs.

- Services not registering in Eureka:
  - Verify `eureka.client.service-url.defaultZone` in service configs.
  - Confirm `service-registry` and `gateway-service` containers are running.

- No metrics in Prometheus/Grafana:
  - Confirm scrape configs in `Infra/infra/prometheus/prometheus.yml`.
  - Check OTEL collector pipelines in `Infra/otel-config.yml`.

- Windows path for OTEL config:
  - Replace the absolute Windows path in Compose with a relative mapping as noted above for portability.

---

## ➕ Extending the System

To add a new microservice:
1. Scaffold a new Spring Boot project (Java 17) with Web, Actuator, Kafka (if needed), and Eureka Client.
2. Add `Dockerfile` and configure health endpoint (`/actuator/health`).
3. Configure Kafka topics and OTEL exporter in `application.yml`.
4. Register with Eureka and add route mappings in the Gateway.
5. Add service to `Infra/docker-compose.yml` with appropriate ports and dependencies.
6. Validate with `docker compose up --build`.

---

## 🤝 Contributing

Issues and PRs are welcome. Open a discussion if you want to add services or improve the stack.

Maintainer: Arun — backend engineer focused on clarity, reliability, and observability.

---

## 📄 License

This repository is provided under the MIT License (or your preferred license). If not present, add a LICENSE file to clarify usage.





