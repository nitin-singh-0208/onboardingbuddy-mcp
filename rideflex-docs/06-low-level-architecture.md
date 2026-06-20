# RideFlex — Low-level architecture (selected modules)

This document zooms into **Booking / Trip Service** and **Driver Matching Service**: internal components, representative persistence, and **sync vs async** integration patterns. All schema fields are **illustrative** for ER diagrams in Markdown viewers.

---

## Booking / Trip Service

### Internal components

| Component | Purpose |
|-----------|---------|
| **Trip API** | REST handlers for create, cancel, status, and driver-visible transitions. |
| **Rental API** | Reservation create/modify/return; coordinates with Fleet for locks. |
| **State engine** | Deterministic state machines for `TRIP` and `RENTAL` aggregates with guard conditions. |
| **Pricing adapter** | Calls Pricing for quotes; stores `pricing_snapshot_id` on commit. |
| **Payment adapter** | Creates payment intents, listens for capture/refund outcomes (sync + event). |
| **Event publisher** | Emits domain events after successful commits (outbox pattern assumed). |
| **Projection builder** | Maintains read models for mobile status polling (optional CQRS-lite). |

### Sync vs async

- **Synchronous REST**: Create trip → validate user → request assignment from Matching (or enqueue if async variant) → return booking confirmation; Pricing quote fetch inline.
- **Asynchronous events**: `TripCompleted` → Notification, Rating eligibility, analytics; `RentalHandoffCompleted` → Fleet telematics correlation (fictional).

### Representative ER diagram (trips and rentals)

```mermaid
erDiagram
  TRIP {
    uuid id PK
    uuid rider_id FK
    uuid driver_id FK
    uuid vehicle_id FK
    text status
    timestamptz requested_at
    timestamptz started_at
    timestamptz ended_at
    uuid pricing_snapshot_id FK
    uuid payment_intent_id FK
    text correlation_id
  }

  RENTAL_RESERVATION {
    uuid id PK
    uuid customer_id FK
    uuid vehicle_id FK
    text status
    timestamptz pickup_at
    timestamptz return_due_at
    timestamptz returned_at
    uuid pricing_snapshot_id FK
    uuid payment_intent_id FK
  }

  TRIP_EVENT {
    bigint id PK
    uuid trip_id FK
    text event_type
    jsonb payload
    timestamptz occurred_at
  }

  TRIP ||--o{ TRIP_EVENT : logs
```

---

## Driver Matching Service

### Internal components

| Component | Purpose |
|-----------|---------|
| **Demand ingress** | Consumes `TripRequested` or receives synchronous assignment RPC from Booking. |
| **Geo index service** | Maintains driver locations in Redis GEO structures per cell/region. |
| **Scorer** | Ranks candidates by distance, ETA, driver tier, fairness rotation, and vehicle match. |
| **Offer manager** | Creates time-boxed offers, tracks accept/decline/timeout. |
| **Reassignment loop** | On failure, re-queries with relaxed constraints up to policy limits. |
| **Policy hooks** | Pluggable rules (fictional) for max concurrent offers per driver. |

### Sync vs async

- **Synchronous**: Booking may call `POST /matching/assign` for low-latency path; response includes `assignment_id` or structured no-supply.
- **Asynchronous**: `DriverLocationUpdated` events from driver apps (via gateway ingestion service not detailed here) stream into Kafka; Matching consumers update Redis. `DriverAssigned` and `AssignmentFailed` published for Booking and Notification.

### Representative ER diagram (matching persistence)

PostgreSQL holds **durable** assignment records and audit; Redis holds **ephemeral** offer TTLs (not shown as ER).

```mermaid
erDiagram
  ASSIGNMENT {
    uuid id PK
    uuid trip_id FK
    uuid driver_id FK
    text status
    timestamptz offered_at
    timestamptz responded_at
    text outcome
  }

  DRIVER_STATE {
    uuid driver_id PK
    text availability
    uuid active_vehicle_id FK
    timestamptz updated_at
  }

  MATCHING_CONFIG {
    uuid id PK
    text region_code
    int offer_timeout_ms
    int max_parallel_offers
    timestamptz effective_from
  }

  ASSIGNMENT }o--|| DRIVER_STATE : targets
```

---

## Cross-service communication summary

```mermaid
flowchart LR
  BT[Booking / Trip Service]
  DM[Driver Matching Service]
  K[(Kafka)]
  R[(Redis GEO)]

  BT -->|sync REST assign| DM
  DM -->|sync response assignment_id| BT
  BT -->|publish TripRequested| K
  K -->|consume| DM
  DM -->|publish DriverAssigned| K
  K -->|consume| BT
  DM <-->|location reads writes| R
```

## Operational cautions (demo)

Matching is **latency-sensitive**; protect Redis with tiered memory policies and backpressure on location write volume. Booking prioritizes **correctness** of money and state; prefer transactional outbox when emitting payment-related events.
