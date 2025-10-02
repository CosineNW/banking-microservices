# Architecture

This system is a microservices-based banking backend designed for modularity, event-driven integration, and observability.

## Components

- API Gateway (Spring Cloud Gateway)
  - Entry point for client traffic, applies filters and routes to backend services.
  - Routes currently configured: /customer/** and /notification/**.

- Service Registry (Eureka)
  - Centralized discovery; services can register to support client-side load balancing and dynamic routing.

- Business services
  - customer-service: CRUD and reward points field on the customer profile.
  - transaction-service: CRUD for transactions; publishes transaction events.
  - reward-service: persists and queries reward history; consumes reward update events.
  - preference-service: manages user notification preferences; publishes updates.
  - notification-service: consumes events and sends email notifications; exposes a test API.

- Event bus
  - Apache Kafka: broker 9092 for inter-service messaging.

- Databases
  - MySQL: customerdb, transactiondb, rewarddb (created by Infra/db-init).
  - H2 (in-memory): preference-service.

- Observability
  - Prometheus + Micrometer: metrics endpoints at /actuator/prometheus.
  - Grafana: dashboards.
  - OpenTelemetry Collector: HTTP receiver (:4318), debug logs exporter enabled.

## Deployment model (local)

Docker Compose (Infra/docker-compose.yml) orchestrates:
- Zookeeper + Kafka
- MySQL + db-init (creates customerdb, transactiondb, rewarddb)
- Prometheus + Grafana
- OTEL Collector
- All services (built from their local Dockerfiles)

Compose uses a bridge network (my-network). Services are reachable from host via published ports (see docs/README.md).

## Request flow (synchronous)

- Client → Gateway → Service
  - Example: Client calls /customer/** on gateway, which routes to customer-service.
  - Health and metrics are exposed via Spring Actuator; gateway adds global request filters.

## Event flow (asynchronous)

Primary topics (current state and intent):
- transaction-events
  - Produced by transaction-service (payload: TransactionEvent).
  - Intended downstream: reward calculation or notification triggers (consumers not included in codebase yet).

- user-preference-updated
  - Produced by preference-service (payload: PreferenceDTO).
  - Consumed by notification-service (PreferenceConsumer prints state; can cache or persist if extended).

- customer-reward-updated
  - Consumed by reward-service and notification-service (payload: CustomerRewardUpdatedEvent).
  - Producer not present in current repo; likely emitted by the component that computes rewards (e.g., customer-service after updating points). See docs/eventing.md for guidance to implement.

## Data model overview

- Customer (customer-service)
  - customerId, name, email, phone, rewardPoints (MySQL)

- Transaction (transaction-service)
  - transactionId, customerId, amount, currency, type, timestamp, description (MySQL, table transactions)

- RewardRecord (reward-service)
  - id, customerId, rewardPoints, receivedAt (MySQL)

- Preference (preference-service)
  - userId, email, sms, push (H2 in-memory)

## Observability model

- Each service exposes:
  - /actuator/health with details
  - /actuator/prometheus for metrics
- Prometheus scrapes host.docker.internal:{service-port} paths /actuator/prometheus.
- Grafana connects to Prometheus (port 9090) and visualizes service counters, timers, Kafka producer metrics, etc.
- OTEL Collector is wired to accept OTLP HTTP (4318) and export to debug logs; to enable distributed tracing in dashboards, add a trace backend (e.g., Jaeger, Tempo) and Spring instrumentation.

## Security and discovery

- Eureka server is enabled in service-registry.
- Gateway is configured as a discovery client; routes are currently static (URI http://localhost:808x).
- Security is not enabled by default; add Spring Security if required.

## Extensibility

- Add new services by following the same pattern (Dockerfile, Actuator, metrics).
- Introduce a reward-calculation consumer for transaction-events to produce customer-reward-updated (see docs/eventing.md).
- Expand gateway routing and apply authentication filters.
- Switch preference-service to persistent storage (MySQL) for production durability.