# Eventing and Kafka Topics

This system uses Apache Kafka for asynchronous communication. Below are the topics, payloads, and producer/consumer mappings reflected in the codebase, along with notes on planned integrations.

## Topics and payloads

1) transaction-events
- Produced by: transaction-service
- Payload: TransactionEvent
- Example JSON:
  {
    "transactionId": "T-2001",
    "customerId": "C-1001",
    "amount": 125.50,
    "type": "DEBIT",
    "timestamp": "2024-12-01T12:00:00Z"
  }

2) user-preference-updated
- Produced by: preference-service
- Consumed by: notification-service (PreferenceConsumer)
- Payload: PreferenceDTO
- Example JSON:
  {
    "userId": "C-1001",
    "email": true,
    "sms": false,
    "push": true
  }

3) customer-reward-updated
- Consumed by: reward-service (RewardConsumer), notification-service (RewardEventListener)
- Produced by: not present in the current repo; intended to be emitted by a service once reward points are computed (e.g., customer-service or a dedicated reward-calculator service)
- Payload: CustomerRewardUpdatedEvent
- Example JSON:
  {
    "customerId": "C-1001",
    "rewardPoints": 12.55,
    "timestamp": "2024-12-01T12:00:00Z"
  }

## Producer/Consumer summary

- transaction-service
  - Producer: TransactionEventProducer → topic transaction-events
  - Counters: kafka.producer.success.count, kafka.producer.failure.count

- preference-service
  - Producer: PreferenceProducer → topic user-preference-updated

- notification-service
  - Consumer: RewardEventListener → topic customer-reward-updated
  - Consumer: PreferenceConsumer → topic user-preference-updated
  - Kafka health check: KafkaHealthIndicator via AdminClient

- reward-service
  - Consumer: RewardConsumer → topic customer-reward-updated
  - Persists RewardRecord (customerId, rewardPoints, receivedAt)

- customer-service
  - Has a local TransactionEventConsumer + TransactionEventHandler to update rewardPoints on customer entity (no Kafka listener wired yet). To integrate with Kafka, add a @KafkaListener for transaction-events.

## Guidance to complete reward event flow

To implement customer-reward-updated emission:
- Option A: Emit from customer-service when reward points are updated in CustomerRewardServiceImpl.
  - Add a KafkaTemplate<String, CustomerRewardUpdatedEvent> and send after updating reward points.
- Option B: Emit from a dedicated reward-calculation microservice that consumes transaction-events, computes rewards, and publishes customer-reward-updated.

Pseudo-steps (Option A):
- Add dependency spring-kafka to customer-service POM (already commented in POM; uncomment as needed).
- Create Kafka config for producer (bootstrap servers, serializers).
- Add CustomerRewardUpdatedEvent DTO shared schema (align with reward-service).
- In CustomerRewardServiceImpl.updateReward:
  - Build event and kafkaTemplate.send("customer-reward-updated", customerId, event)

## Serialization and compatibility

- Current consumers use String payload and ObjectMapper for deserialization (reward-service, notification-service).
- For robust contracts, consider:
  - Schema registry (Avro/JSON Schema)
  - Versioned DTOs with backward compatibility
  - Using Spring Kafka JsonSerializer/JsonDeserializer with trusted packages configured

## Operational notes

- Broker addresses:
  - In Docker: kafka:9092
  - On host: localhost:9092
- For local testing without compose, ensure Kafka is running and the topic exists (Spring can auto-create topics or use admin clients).