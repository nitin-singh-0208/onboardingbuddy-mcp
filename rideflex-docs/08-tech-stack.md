# RideFlex — Technology stack

RideFlex’s **reference stack** is chosen for mainstream hiring, strong ecosystem maturity, and clear separation of transactional storage, streaming, and in-memory acceleration. This is a **demo baseline**, not a mandate for every edge deployment.

---

## Application services

- **Language**: Java **21** (LTS) for service uniformity and modern concurrency primitives.
- **Framework**: **Spring Boot** for REST APIs, validation, observability hooks, and integration with Spring Kafka / Spring Data.
- **API style**: REST for mobile and gateway; internal gRPC is **optional** and not assumed in onboarding docs.
- **Build**: Gradle with convention plugins; container images built as **distroless** or minimal JRE images (team choice).

---

## Messaging and integration

- **Apache Kafka**: Primary **event backbone** for domain choreography (`TripRequested`, `DriverAssigned`, `PaymentCaptured`, etc.).
- **Schema governance**: **JSON Schema** or **Protobuf** (fictional team choice) enforced at publish time via a schema registry pattern.
- **Idempotency**: Producers attach `event_id`; consumers maintain idempotency keys for at-least-once delivery.

---

## Data persistence

- **PostgreSQL**: System of record per service (users, trips, rentals, payments, fleet, ratings). Migrations via Flyway or Liquibase (unspecified).
- **Redis**: **Geo** structures for driver locations, rate-limit counters at gateway edge (optional), hot pricing rule caches, and short-lived matching offers.
- **Object storage** (optional extension): Presigned uploads for driver documents and vehicle imagery—referenced but not vendor-specific here.

---

## Cross-cutting platform concerns

- **Kubernetes**: Default deployment target; **Helm** or **Kustomize** for environment overlays (dev/stage/prod).
- **Ingress**: Regional L7 load balancers terminating TLS; **cert-manager** for certificate lifecycle (fictional ops detail).
- **Observability**: OpenTelemetry traces propagated from gateway; **Prometheus** metrics; **Grafana** dashboards; structured JSON logs with `trace_id`.
- **Secrets**: Mounted from a secrets manager; no plaintext credentials in repos (enforced by onboarding checklist).

---

## Client applications

- **Mobile**: Native **Swift** (iOS) and **Kotlin** (Android) with shared design tokens; cross-platform frameworks intentionally **not** assumed.
- **Admin web**: **React** + TypeScript SPA talking only to gateway-backed BFF endpoints (fictional split).

---

## Brief deployment note (Kubernetes)

Each microservice ships as a **Deployment** with **PodDisruptionBudgets** for core paths (Booking, Matching, Payment). **HPA** scales Matching on CPU and custom metrics (Kafka consumer lag). **Regional pairs** are described at a high level only: active-active for reads where safe; **Kafka** replication across AZs within a region.

---

## What is intentionally not specified

- Exact cloud provider, service mesh adoption, and feature flag vendor are left as **TBD** so onboarding exercises can substitute plausible answers without contradicting a canonical vendor list.

---

## Local development (typical engineer experience)

Engineers usually run **selected services** plus **Docker Compose** bundles providing PostgreSQL, Redis, Kafka (or Redpanda as a lighter substitute in local sandboxes), and a **wiremock** stand-in for the payment service provider. Spring profiles `local` and `integration` separate fast iteration from CI pipelines that spin ephemeral databases and run contract tests against producer-owned API examples. Mobile developers point devices at a **tunnelled** gateway URL; secrets never live in repository roots.

---

## Quality gates before merge (representative)

- **Unit tests** for domain logic and pricing edge cases.
- **Integration tests** with Testcontainers for PostgreSQL and Kafka where event contracts matter.
- **Static analysis** (SpotBugs, Checkstyle or Error Prone equivalents) aligned with org defaults.
- **Container scan** in CI for critical CVEs on base images.

These gates are described generically so the demo corpus stays vendor-agnostic while still sounding like a real engineering handbook section.
