# BookIt — Production-Grade Booking System

A production-style, microservices booking platform (flight/hotel/cinema) built with **Java 21 + Spring Boot 3**, **Angular 17**, **PostgreSQL**, **Redis**, **Kafka**, and **Spring AI (GPT-4o-mini)**.

---

## Architecture Overview

```
                         ┌─────────────────────────────────────────────┐
                         │              Angular Frontend                │
                         │   Login · Search · Book · Dashboard · Chat   │
                         └────────────────────┬────────────────────────┘
                                              │ HTTPS
                         ┌────────────────────▼────────────────────────┐
                         │              API Gateway  :8080              │
                         │  JWT validation · Rate limiting · Routing    │
                         │  Circuit Breaker (Resilience4j)              │
                         └─────┬─────────┬────────┬────────┬───────────┘
                               │         │        │        │
             ┌─────────────────▼──┐  ┌───▼──┐ ┌──▼───┐ ┌──▼───────────┐
             │  User Service :8081│  │Inven-│ │Book- │ │  Payment     │
             │  Auth · JWT · Redis│  │tory  │ │ing   │ │  Service     │
             │  BCrypt · Flyway   │  │:8082 │ │:8083 │ │  :8084       │
             └────────────────────┘  └──┬───┘ └──┬───┘ └──────┬───────┘
                                        │        │             │
                              ┌──────────▼────────▼─────────────▼──────┐
                              │               Apache Kafka               │
                              │  booking-created · payment-completed     │
                              │  payment-failed · booking-cancelled      │
                              │  + Retry topics + Dead Letter Topics    │
                              └──────────────────────┬──────────────────┘
                                                     │
                                  ┌──────────────────┴──────────────────┐
                                  │                                      │
                       ┌──────────▼──────────┐       ┌──────────────────▼──────┐
                       │  Notification :8085  │       │     AI Service :8086    │
                       │  Email (SMTP/SES)    │       │  Spring AI · GPT-4o-mini│
                       └─────────────────────┘       │  Chat · Recommendations │
                                                      │  Function-calling tools │
                                                      └─────────────────────────┘
```

---

## Microservices

| Service | Port | Database | Responsibility |
|---------|------|----------|---------------|
| API Gateway | 8080 | Redis | Routing, auth, rate-limiting |
| User Service | 8081 | PostgreSQL `userdb` | Register, login, JWT |
| Inventory Service | 8082 | PostgreSQL `inventorydb` | Seat/room availability & locking |
| Booking Service | 8083 | PostgreSQL `bookingdb` | Booking lifecycle, saga orchestration |
| Payment Service | 8084 | PostgreSQL `paymentdb` | Payment processing, circuit breaker |
| Notification Service | 8085 | — | Email on booking events |
| AI Service | 8086 | — (in-memory) | Conversational assistant, recommendations |

---

## AI Service

The AI Service (`ai-service`, port **8086**) adds a conversational layer on top of the booking platform using **Spring AI** and **OpenAI GPT-4o-mini**.

### Features

| Feature | Details |
|---------|---------|
| Conversational assistant | Multi-turn chat with in-memory conversation history (last 20 messages per session) |
| Function-calling tools | `searchAvailability` calls Inventory Service; `getBookingStatus` calls Booking Service; `getBookingFAQ` serves FAQ answers locally |
| Booking recommendations | Structured JSON recommendations (summary, tips, price range, top picks) based on type, date, and budget |
| Quick insight | One-shot question endpoint with no conversation memory |

### How the assistant works

```
User message
     │
     ▼
ChatClient (Spring AI)
     │  system prompt: BookIt AI persona + today's date + available inventory types
     │  memory advisor: injects last N messages from in-memory store (per conversationId)
     ├─► Tool: searchAvailability  → GET http://inventory-service:8082/api/inventory/availability
     ├─► Tool: getBookingStatus    → GET http://booking-service:8083/api/bookings/{id}
     └─► Tool: getBookingFAQ       → local switch (cancellation / payment / booking / general)
     │
     ▼
ChatResponse { reply, conversationId, suggestions[] }
```

GPT-4o-mini decides when to call which tool. The `conversationId` returned on the first call should be sent on subsequent calls to maintain context.

---

## Saga Pattern — Booking Flow

```
User → POST /api/bookings
         │
         ▼
  Booking Service ──publishes──► [booking-created]
         │                              │
         │                    ┌─────────┴──────────┐
         │                    ▼                    ▼
         │          Inventory Service       Payment Service
         │         (hold inventory)     (charge card via gateway)
         │                    │                    │
         │                    │    ┌───────────────┘
         │                    │    │ payment-completed / payment-failed
         │                    │    ▼
         │          Booking Service (CONFIRMED / FAILED)
         │          Inventory Service (BOOKED / AVAILABLE)
         │          Notification Service (email sent)
         │
         ▼
  Compensating actions on failure:
    payment-failed → Inventory released → Booking marked FAILED → User notified
```

---

## Concurrency & Double-Booking Prevention

Three layers of protection:

1. **Redis Distributed Lock** (`RedisDistributedLock.java`)  
   Before touching the DB, a `SET NX EX` lock is acquired per `itemId`. Concurrent requests for the same item queue at the Redis layer. Lock is released via atomic Lua script to prevent accidental release by another caller.

2. **Pessimistic DB Lock** (`@Lock(LockModeType.PESSIMISTIC_WRITE)`)  
   `findByIdWithLock()` issues `SELECT ... FOR UPDATE`. Even if two requests slip past Redis, only one holds the row lock.

3. **Optimistic Locking** (`@Version` on `InventoryItem`)  
   A version counter on each row ensures any concurrent update after a stale read throws `OptimisticLockingFailureException`, caught by `@Retryable`.

**Expired hold cleanup**: A scheduled job (`@Scheduled(fixedDelay=120_000)`) runs every 2 minutes to release holds where `holdExpiresAt < NOW()`, preventing ghost-holds from crashes.

---

## Idempotency

Both Booking and Payment APIs accept a client-supplied `idempotencyKey`:

- On first call: persisted to DB with `UNIQUE` constraint
- On retry: DB lookup returns the existing record instantly (no re-processing)
- Race condition (concurrent identical keys): caught via `DataIntegrityViolationException` → fallback lookup

Frontend generates a `UUID v4` per booking attempt (not per page load), so network retries reuse the same key but page refreshes get a new one.

---

## Kafka Event Design

| Topic | Producer | Consumers | On failure |
|-------|----------|-----------|------------|
| `booking-created` | Booking Service | Inventory, Payment, Notification | Retry ×4 → DLT |
| `payment-completed` | Payment Service | Booking, Inventory, Notification | Retry ×4 → DLT |
| `payment-failed` | Payment Service | Booking, Inventory, Notification | Retry ×4 → DLT |
| `booking-cancelled` | Booking Service | Inventory, Notification | Retry ×4 → DLT |
| `*.DLT` | Kafka retry infra | Manual/Alert handler | PagerDuty alert |

**Retry strategy**: Exponential backoff — 1s, 2s, 4s, 8s (max 10s) before routing to DLT.  
**Kafka producer**: `acks=all`, `enable.idempotence=true` prevents producer-level duplicates.

---

## Security

- **JWT (HMAC-256)**: 15-minute access tokens, 7-day refresh tokens stored in Redis
- **Refresh token rotation**: Every `/auth/refresh` call invalidates the old refresh token (replay attack prevention)
- **BCrypt (strength 12)**: Password hashing
- **Rate limiting**: Redis token bucket — 100 req/s per user, burst to 200
- **RBAC**: `@PreAuthorize("hasRole('ADMIN')")` on admin endpoints
- **Downstream trust**: Services trust `X-User-Id` / `X-User-Role` headers set by the gateway; JWT is not re-validated inside the cluster

---

## Database Schema

### `users`
```sql
id UUID PK, email UNIQUE, password_hash, first_name, last_name,
role {USER|ADMIN}, active BOOL, created_at, updated_at
```
Indexes: `email` (unique), `active` (partial — active=TRUE only)

### `inventory_items`
```sql
id UUID PK, reference_id UUID, item_type, label, available_date DATE,
status {AVAILABLE|HELD|BOOKED|CANCELLED}, booking_id UUID,
hold_expires_at TIMESTAMPTZ, price NUMERIC, version BIGINT
```
Indexes: `(item_type, available_date, status)`, `(reference_id, available_date)`,
`booking_id`, `hold_expires_at WHERE status='HELD'`

### `bookings`
```sql
id UUID PK, user_id UUID, reference_id UUID, booking_type,
inventory_item_id UUID, booking_date DATE,
status {PENDING|INVENTORY_HELD|PAYMENT_PROCESSING|CONFIRMED|CANCELLED|FAILED},
total_amount NUMERIC, idempotency_key UNIQUE, failure_reason TEXT
```
Indexes: `(user_id, created_at DESC)`, `status`, `(reference_id, booking_date)`

### `payments`
```sql
id UUID PK, booking_id UUID, user_id UUID, amount NUMERIC,
currency, status {PENDING|PROCESSING|COMPLETED|FAILED|REFUNDED},
payment_method, payment_reference, gateway_transaction_id,
idempotency_key UNIQUE, failure_reason TEXT
```
Indexes: `booking_id`, `user_id`, `status`

---

## Observability

| Concern | Tool | How to access |
|---------|------|--------------|
| Metrics | Micrometer + Prometheus | Each service exposes `/actuator/prometheus` |
| Health | Spring Actuator | Each service exposes `/actuator/health` |
| Logs | Structured (MDC traceId) | stdout — pipe to your preferred log aggregator |

Prometheus can be run locally and pointed at the service ports using `observability/prometheus.yml`.

Structured log format:
```
2025-05-11T10:30:00Z [http-nio-1] [traceId=abc123] INFO  BookingService - Booking created: bookingId=..., userId=...
```

---

## Scaling Strategy

### Horizontal scaling
All services are stateless — run multiple instances behind a load balancer.  
State lives in PostgreSQL (durability), Redis (sessions/locks), Kafka (events).

### Database
- **Connection pooling**: HikariCP configured per service (20–30 max connections)
- **Read replicas**: Route `@Transactional(readOnly=true)` queries to replica via Spring routing datasource
- **Partitioning**: `inventory_items` can be range-partitioned on `available_date`
- **Separate DBs per service**: No cross-DB joins; each service owns its schema

### Caching
- Availability queries cached in Redis with 60s TTL, evicted on inventory state change
- User sessions (refresh tokens) in Redis with sliding expiry

### Kafka
- Increase partition count on hot topics (`booking-created`, `payment-completed`)
- Each consumer group scales independently — add instances, Kafka rebalances

### Bottlenecks under 1M+ users
1. **Payment gateway**: Single point — add queue + async processing, multiple gateway accounts
2. **Inventory lock contention**: Shard items into `inventoryItemId % N` Redis lock buckets
3. **Notification**: Already async via Kafka; add worker pool per event type
4. **DB write hot spots**: Use `COPY` for bulk inserts, connection pool tuning, PgBouncer

---

## Getting Started

### Prerequisites

- **Java 21** — [sdkman.io](https://sdkman.io) is the easiest way to install
- **Maven 3.9+** (or use the `./mvnw` wrapper included in each service)
- **PostgreSQL 16** — install locally or via Homebrew: `brew install postgresql@16`
- **Redis 7** — `brew install redis`
- **Apache Kafka 3.6** — download from [kafka.apache.org](https://kafka.apache.org/downloads) or `brew install kafka`
- **Node 20 + Angular CLI** — for the frontend

### 1. Start infrastructure

```bash
# PostgreSQL — create one DB per service
psql -U postgres -c "CREATE DATABASE userdb;"
psql -U postgres -c "CREATE DATABASE inventorydb;"
psql -U postgres -c "CREATE DATABASE bookingdb;"
psql -U postgres -c "CREATE DATABASE paymentdb;"

# Redis (runs on port 6379 by default)
redis-server

# Kafka — from your Kafka install directory
bin/zookeeper-server-start.sh config/zookeeper.properties &
bin/kafka-server-start.sh config/server.properties &
```

### 2. Run backend services

Open a terminal per service (or use your IDE's run configurations):

```bash
# Terminal 1
cd user-service && ./mvnw spring-boot:run

# Terminal 2
cd inventory-service && ./mvnw spring-boot:run

# Terminal 3
cd booking-service && ./mvnw spring-boot:run

# Terminal 4
cd payment-service && ./mvnw spring-boot:run

# Terminal 5
cd notification-service && ./mvnw spring-boot:run

# Terminal 6
cd api-gateway && ./mvnw spring-boot:run

# Terminal 7 (requires OPENAI_API_KEY)
export OPENAI_API_KEY=<your-key>
cd ai-service && ./mvnw spring-boot:run
```

Flyway migrations run automatically on startup and create all tables.

### 3. Run the frontend

```bash
cd frontend
npm install
npm start   # http://localhost:4200
```

### Service URLs

| Service | URL |
|---------|-----|
| Angular App | http://localhost:4200 |
| API Gateway | http://localhost:8080 |
| User Service | http://localhost:8081/actuator/health |
| Inventory Service | http://localhost:8082/actuator/health |
| Booking Service | http://localhost:8083/actuator/health |
| Payment Service | http://localhost:8084/actuator/health |
| Notification Service | http://localhost:8085/actuator/health |
| AI Service | http://localhost:8086/actuator/health |

### Environment variables

All services use sensible `localhost` defaults. Override with environment variables when needed:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/userdb
export DB_USER=postgres
export DB_PASSWORD=yourpassword
export REDIS_HOST=localhost
export KAFKA_BROKERS=localhost:9092
export JWT_SECRET=your-256-bit-secret-minimum-32-characters

# AI Service
export OPENAI_API_KEY=sk-...                          # required
export INVENTORY_SERVICE_URL=http://localhost:8082    # defaults to localhost
export BOOKING_SERVICE_URL=http://localhost:8083      # defaults to localhost
```

---

## API Reference (Key Endpoints)

```
POST   /api/auth/register          Register user
POST   /api/auth/login             Login, returns JWT
POST   /api/auth/refresh           Refresh access token
GET    /api/users/me               Current user profile

GET    /api/inventory/availability?referenceId=&date=&type=   Search available items

POST   /api/bookings               Create booking (idempotent)
GET    /api/bookings               My booking history (paginated)
GET    /api/bookings/{id}          Single booking
DELETE /api/bookings/{id}          Cancel booking

POST   /api/payments               Process payment (idempotent)
GET    /api/payments/booking/{id}  Payment for a booking

# AI Service  (port 8086 — direct, not routed through gateway)
POST   /api/ai/chat                Conversational assistant (multi-turn)
                                   Body: { "message": "...", "conversationId": "...", "userId": "..." }
                                   Returns: { "reply": "...", "conversationId": "...", "suggestions": [...] }

POST   /api/ai/recommendations     AI-generated recommendations
                                   Body: { "bookingType": "FLIGHT_SEAT|HOTEL_ROOM|CINEMA_SEAT",
                                           "date": "YYYY-MM-DD", "preferences": "...", "budget": "..." }
                                   Returns: { "summary", "tips", "bestTimeToBook",
                                              "estimatedPriceRange", "topPicks" }

GET    /api/ai/insight?question=   One-shot question (no conversation memory)
```

---

## Architecture Decisions & Trade-offs

| Decision | Choice | Trade-off |
|----------|--------|-----------|
| Database per service | Separate PostgreSQL instances | Higher infra cost vs. clear boundaries |
| Saga (choreography) | Kafka events | No central coordinator needed; harder to trace end-to-end |
| Optimistic + Pessimistic lock | Both layers | Slight write overhead; eliminates double-booking under any race |
| JWT stateless + Redis refresh | Hybrid | Balance between stateless scaling and token revocation capability |
| Idempotency via DB unique key | Application-level | Simple, correct; requires client to manage idempotency keys |
| Single Redis node | One Redis | Sufficient when DB locking is the true safety net; Redlock for stricter needs |
| AI conversation memory | In-memory (`MessageWindowChatMemory`, last 20 messages) | Zero infra cost; lost on restart — swap `InMemoryChatMemoryRepository` for Redis/DB-backed store for persistence |
| AI function-calling | Spring AI `@Tool` on `InventoryTools` / `BookingTools` | Model decides which tool to invoke; keeps AI layer decoupled from internal service contracts |
