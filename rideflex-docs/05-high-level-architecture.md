# RideFlex — High-level architecture

RideFlex clients include **rider** and **driver** mobile applications, a **rental customer** app (could be a mode inside the rider app in some deployments), and **admin/ops** web consoles. Traffic enters through a regional **API gateway** that terminates TLS, validates tokens with the **User Service** (or shared IdP), applies rate limits, and routes to microservices. Core business operations persist primarily in **PostgreSQL** per service (database-per-service pattern). A **Kafka** cluster carries domain events for choreography between services. **Redis** supports geo queries, hot configuration, and ephemeral matching state.

## System narrative

1. Mobile and web clients call the gateway over HTTPS.
2. The gateway forwards REST-style requests to domain services; most responses are synchronous.
3. On state changes (e.g., trip requested, driver assigned), services publish events to Kafka.
4. Downstream consumers (Notification, analytics adapters, search indexers) react without blocking the user path.
5. Each owning service writes to its **primary PostgreSQL** database; cross-service queries go through APIs, not shared tables.

Operational deployment is assumed to be **Kubernetes** per region, with horizontal pod autoscaling on Matching and Pricing read paths. Details appear in `08-tech-stack.md`.

---

## System context diagram (logical connectivity)

The flowchart below is **logical** (not every network hop or sidecar is shown). Solid arrows imply primary request/response paths; the broker implies publish/subscribe fan-out.

```mermaid
flowchart TB
  subgraph Clients
    RA[Rider app]
    DA[Driver app]
    RCA[Rental customer app]
    AW[Admin web]
  end

  GW[API Gateway]

  subgraph Core_services
    US[User Service]
    BT[Booking / Trip Service]
    DM[Driver Matching Service]
    PR[Pricing Service]
    PY[Payment Service]
    FV[Fleet and Vehicle Mgmt]
    NS[Notification Service]
    RR[Rating and Review Service]
  end

  MB[(Kafka / message broker)]

  subgraph Data_stores
    PG1[(PostgreSQL - users)]
    PG2[(PostgreSQL - trips and rentals)]
    PG3[(PostgreSQL - matching config)]
    PG4[(PostgreSQL - pricing rules)]
    PG5[(PostgreSQL - payments)]
    PG6[(PostgreSQL - fleet)]
    PG7[(PostgreSQL - ratings)]
    RD[(Redis - geo and cache)]
  end

  RA --> GW
  DA --> GW
  RCA --> GW
  AW --> GW

  GW --> US
  GW --> BT
  GW --> DM
  GW --> PR
  GW --> PY
  GW --> FV
  GW --> RR

  US --> PG1
  BT --> PG2
  DM --> PG3
  DM --> RD
  PR --> PG4
  PY --> PG5
  FV --> PG6
  RR --> PG7

  BT <--> DM
  BT --> PR
  BT --> PY
  BT --> FV
  BT --> NS
  DM --> NS
  PY --> NS
  RR --> NS

  US --> MB
  BT --> MB
  DM --> MB
  PR --> MB
  PY --> MB
  FV --> MB
  RR --> MB
  NS --> MB
```

## Design notes for onboarding

- **Gateway thickness**: Business rules belong in services, not the gateway, so gateway replacements remain low-risk.
- **Broker as backbone**: Kafka topics are named by domain (`rideflex.trip.*`, `rideflex.rental.*`) in implementation guides not included here.
- **Failure isolation**: If Notification lags, core booking still completes; user-visible delay may occur on auxiliary channels only.

This document pairs with `06-low-level-architecture.md` for two-service drill-downs and with `07-flows.md` for sequence-level behavior.
