# Observability and Metrics

This system exposes health and metrics across all microservices and ships with Prometheus and Grafana via docker-compose. OpenTelemetry Collector is configured for OTLP HTTP ingestion with a debug logs exporter.

## Actuator endpoints

All services (gateway, customer, transaction, reward, preference, notification, service-registry):
- /actuator/health (with details)
- /actuator/prometheus (Micrometer metrics endpoint)
- Some services also expose /actuator/info

Prometheus scrape config (Infra/infra/prometheus/prometheus.yml) targets:
- host.docker.internal:8081 → customer-service
- host.docker.internal:8082 → transaction-service
- host.docker.internal:8083 → reward-service
- host.docker.internal:8084 → notification-service
- host.docker.internal:8085 → gateway-service
- host.docker.internal:8086 → preference-service
- host.docker.internal:8761 → service-registry
with metrics_path /actuator/prometheus.

## Prometheus and Grafana

- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000
  - Add Prometheus as a data source (URL: http://prometheus:9090 inside compose, or http://localhost:9090 from host).
  - Create dashboards for service metrics, Kafka producer counters, timers, etc.

## Metrics of interest

- transaction-service:
  - kafka.producer.success.count
  - kafka.producer.failure.count

- reward-service:
  - rewardservice_reward_save_total (Counter)
  - rewardservice_reward_save_duration_seconds (Timer)

- notification-service:
  - notification_kafka_events_total
  - notification_kafka_errors_total

- Gateway:
  - Standard Spring Cloud Gateway metrics (via Micrometer)
  - Global filter spans if tracing is enabled

## OpenTelemetry Collector

Config: Infra/otel-config.yml
- Receivers:
  - otlp http endpoint: 0.0.0.0:4318
- Exporters:
  - debug (verbosity: detailed)
- Pipelines:
  - logs only (receives OTLP and exports to debug log)

Compose publishes OTEL Collector at:
- Host port 4319 → container port 4318
Gateway is configured with:
- OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318

To enable full distributed tracing:
- Add an OTEL trace backend (e.g., Jaeger, Tempo), configure an exporter in OTEL Collector.
- Add Spring instrumentation (Micrometer tracing bridge or OpenTelemetry Spring Boot starter) for services that should emit spans.
- Ensure service configs set OTLP endpoint and sampling levels.

## Health indicators

- customer-service:
  - CustomerDbHealthIndicator (checks JPA repository ping)

- notification-service:
  - KafkaHealthIndicator (AdminClient.describeCluster)

- gateway-service:
  - GatewayHealthIndicator (downstream health check against preference-service)

## Logging

- Services include logback settings and optional logstash encoder dependencies for structured logs (JSON).
- OTEL logging exporter present in notification-service POM (optional).

## Troubleshooting

- Metrics not visible:
  - Verify /actuator/prometheus returns metrics.
  - Check Prometheus targets page for UP state and correct endpoints.

- Kafka connectivity:
  - Ensure broker is reachable:
    - Inside compose: kafka:9092
    - From host: localhost:9092
  - Check notification-service KafkaHealthIndicator in /actuator/health.

- MySQL connectivity:
  - DBs are created by db-init in compose (customerdb, transactiondb, rewarddb).
  - Verify datasource URLs in each service’s application.yml.