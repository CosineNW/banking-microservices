# Service Reference

This document summarizes each microservice: purpose, ports, API endpoints, storage, and event integrations. Host ports assume docker-compose in Infra/, otherwise use container ports.

## service-registry (Eureka)

- Port: 8761 (host: 8762 via compose)
- Purpose: Service discovery; dashboard at /.
- Actuator: /actuator/health, /actuator/prometheus
- Important config (application.yml):
  - eureka.client.register-with-eureka: false
  - eureka.client.fetch-registry: false
- Dockerfile exposes 8761.

### Run and check (compose)
- Eureka dashboard: http://localhost:8762

---

## gateway-service

- Port: 8085 (host: 8185)
- Purpose: Single entry point routing http requests to backend services, global filters, and health checks.
- Routes:
  - /notification/** → http://localhost:8084
  - /customer/** → http://localhost:8081
- Actuator: /actuator/health, /actuator/prometheus
- Discovery: @EnableDiscoveryClient enabled; gateway also has eureka client configuration.
- Notable classes:
  - RouteLocatorConfig: configures routes.
  - TraceGatewayFilter: global filter using Micrometer Tracer.
  - GatewayHealthIndicator: calls downstream health endpoints.

### Example
- Gateway health: GET http://localhost:8185/actuator/health
- Via gateway → customer service: GET http://localhost:8185/customer/api/customers

---

## customer-service

- Port: 8081 (host: 8181)
- Purpose: Customer CRUD and reward points storage.
- Storage: MySQL (customerdb)
- Endpoints:
  - POST /api/customers/create
  - GET /api/customers/{customerId}
  - GET /api/customers
- Entities:
  - Customer: customerId, name, email, phone, rewardPoints
- Repository:
  - CustomerRepository extends JpaRepository<Customer, String>
- Reward integration (local):
  - TransactionEventConsumer + TransactionEventHandler update rewardPoints based on incoming event payload (no Kafka listener wired in code; designed for future use).
- Actuator:
  - /actuator/health (includes CustomerDbHealthIndicator)
  - /actuator/prometheus

### cURL (compose port)
- Create:
  curl -X POST http://localhost:8181/api/customers/create \
    -H "Content-Type: application/json" \
    -d '{"customerId":"C-1001","name":"Alice","email":"alice@example.com","phone":"1234567890","rewardPoints":0}'
- Get by ID:
  curl http://localhost:8181/api/customers/C-1001

---

## transaction-service

- Port: 8082 (host: 8182)
- Purpose: Transaction CRUD and event publishing.
- Storage: MySQL (transactiondb), table transactions
- Endpoints:
  - POST /api/transactions/save
  - GET /api/transactions/{id}
  - GET /api/transactions
  - GET /api/transactions/customer/{customerId}
- Events:
  - Produces to Kafka topic transaction-events (payload: TransactionEvent)
  - Producer metrics: kafka.producer.success.count, kafka.producer.failure.count
- Actuator:
  - /actuator/health, /actuator/prometheus

### cURL
- Create transaction:
  curl -X POST http://localhost:8182/api/transactions/save \
    -H "Content-Type: application/json" \
    -d '{"transactionId":"T-2001","customerId":"C-1001","amount":125.50,"currency":"USD","type":"DEBIT","timestamp":"2024-12-01T12:00:00Z","description":"POS"}'
- Query by customer:
  curl http://localhost:8182/api/transactions/customer/C-1001

---

## reward-service

- Port: 8083 (host: 8183)
- Purpose: Save and query reward history records; consume reward update events.
- Storage: MySQL (rewarddb)
- Endpoints:
  - POST /api/rewards/save (debug stub)
  - GET /api/rewards/{customerId}
- Events:
  - Consumes customer-reward-updated (payload: CustomerRewardUpdatedEvent)
  - Saves record with rewardService.saveReward(event)
- Micrometer metrics used:
  - rewardservice_reward_save_total (Counter)
  - rewardservice_reward_save_duration_seconds (Timer)
- Actuator:
  - /actuator/health, /actuator/prometheus

### cURL
- Query:
  curl http://localhost:8183/api/rewards/C-1001

---

## preference-service

- Port: 8086 (host: 8186)
- Purpose: Store user notification preferences; publish updates to Kafka.
- Storage: H2 in-memory DB (configure to MySQL for persistence in prod)
- Endpoints:
  - GET /preferences/{userId}
  - PUT /preferences/save
- Events:
  - Produces to topic user-preference-updated (payload: PreferenceDTO)
- Actuator:
  - /actuator/health, /actuator/prometheus

### cURL
- Update:
  curl -X PUT http://localhost:8186/preferences/save \
    -H "Content-Type: application/json" \
    -d '{"userId":"C-1001","email":true,"sms":false,"push":true}'

---

## notification-service

- Port: 8084 (host: 8184)
- Purpose: Consume events and send notifications via providers (Email).
- Endpoints:
  - POST /api/notify/send (dispatches a NotificationRequest)
- Events:
  - Consumes customer-reward-updated (RewardEventListener)
  - Consumes user-preference-updated (PreferenceConsumer)
- Providers:
  - EmailNotificationProvider → uses Spring JavaMailSender with smtp.gmail.com
- Actuator:
  - /actuator/health, /actuator/prometheus
- Kafka health indicator:
  - Checks broker connectivity via AdminClient

### cURL
- Send test email (requires EMAIL_USERNAME/EMAIL_PASSWORD configured):
  curl -X POST http://localhost:8184/api/notify/send \
    -H "Content-Type: application/json" \
    -d '{"recipient":"alice@example.com","subject":"Hello","body":"Test message"}'

---

## Environment variables and configuration hints

- Infra/docker-compose.yml sets:
  - Datasource URLs for MySQL:
    - customer-service: jdbc:mysql://mysql:3306/customerdb
    - transaction-service: jdbc:mysql://mysql:3306/transactiondb
    - reward-service: jdbc:mysql://mysql:3306/rewarddb
  - Kafka:
    - Bootstrap servers: kafka:9092 (container), localhost:9092 (host)
  - Gateway:
    - OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318
    - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://service-registry:8761/eureka
  - Notification service:
    - EMAIL_USERNAME, EMAIL_PASSWORD via notification-service/.env
    - KAFKA_BOOTSTRAP_SERVERS, KAFKA_GROUP_ID via .env or application.yml