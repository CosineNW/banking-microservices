# Local Development

This guide shows how to run and test services locally (without docker-compose) and how to use the provided compose stack.

## Option A: Run everything via docker-compose

1) Build all JARs (from repo root):
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

3) Verify key UIs:
   - Prometheus: http://localhost:9090
   - Grafana: http://localhost:3000
   - Eureka: http://localhost:8762
   - Gateway: http://localhost:8185

4) Test APIs using the host ports (see docs/README.md for examples).

## Option B: Run services directly (without containers)

Prerequisites:
- MySQL running locally with databases customerdb, transactiondb, rewarddb
- Kafka running locally on localhost:9092
- Java 17/21 and Maven

1) Start MySQL and create DBs:
   - CREATE DATABASE customerdb;
   - CREATE DATABASE transactiondb;
   - CREATE DATABASE rewarddb;

2) Start Kafka and Zookeeper (standard local setup).

3) Start services (each in its directory):
   - mvn spring-boot:run

4) Default ports:
   - customer-service: 8081
   - transaction-service: 8082
   - reward-service: 8083
   - notification-service: 8084
   - gateway-service: 8085
   - preference-service: 8086
   - service-registry: 8761

5) Update config if needed:
   - application.yml uses localhost:9092 for Kafka and localhost MySQL URLs when run outside compose.
   - For notification-service, set EMAIL_USERNAME and EMAIL_PASSWORD environment variables for SMTP.

## Environment variables and configuration

- notification-service:
  - EMAIL_USERNAME, EMAIL_PASSWORD
  - KAFKA_BOOTSTRAP_SERVERS, KAFKA_GROUP_ID (optional; defaults in application.yml)

- gateway-service:
  - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE (defaults to http://localhost:8761/eureka)
  - OTEL_EXPORTER_OTLP_ENDPOINT (set to http://localhost:4318 if running OTEL locally)

- preference-service:
  - kafka.bootstrap-servers (localhost:9092 by default)
  - kafka.topic.preference=user-preference-updated

## Testing and health

- Actuator:
  - GET /actuator/health (each service)
  - GET /actuator/prometheus (metrics)

- Kafka topics:
  - transaction-events
  - user-preference-updated
  - customer-reward-updated (consumers present; producer to be implemented)

## Common issues

- “Connection refused” to Kafka:
  - Ensure the broker is running and reachable on localhost:9092.

- MySQL authentication:
  - Credentials are root/root in compose; adjust for local MySQL.

- Missing JAR in Docker build:
  - Always run mvn package before docker compose up; Dockerfiles copy target/*.jar.

- Prometheus scraping:
  - When running outside compose, Prometheus config targets host.docker.internal; update to localhost or run compose for observability.