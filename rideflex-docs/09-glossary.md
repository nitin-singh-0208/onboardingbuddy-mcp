# RideFlex — Glossary

Short definitions for new joiners searching the RideFlex demo corpus. All terms are **platform-internal or industry-generic**; they do not reference any real company’s proprietary vocabulary.

When extending this file for retrieval experiments, keep definitions **single-sentence** and avoid linking to external trademarks; prefer linking to sibling Markdown files using relative paths (for example `./05-high-level-architecture.md`) so local viewers resolve predictably.

| Term | Definition |
|------|------------|
| **ETA** | Estimated time of arrival—the predicted minutes until pickup or destination based on traffic models and live driver movement. |
| **Surge pricing** | Temporary fare multiplier applied when demand outstrips nearby supply in a zone, shown to riders before confirmation. |
| **Geofencing** | Virtual boundary on a map used to enforce rules (pricing, availability, airport queues, rental lot access). |
| **Driver utilization** | Share of online time spent on paid activities (en route, on trip) versus idle; used in ops reporting, not as a single moral metric. |
| **Heat map** | Visual demand indicator in the driver app derived from recent trip requests and forecasted imbalance (simplified in demos). |
| **Pricing snapshot** | Immutable record of inputs and outputs from Pricing at booking time; billing disputes reference this, not live rules. |
| **Assignment** | A time-bounded offer linking a specific driver (and vehicle) to a trip; has outcomes like accepted, declined, or expired. |
| **Outbox pattern** | Reliable publishing strategy: write business row and outbound event stub in one DB transaction; separate publisher drains to Kafka. |
| **Authorization hold** | Payment funds reserved but not captured—common before trip completion or for rental deposits. |
| **CQRS-lite** | Pragmatic separation of write model (transactional) and read model (polling-optimized) without full event sourcing. |
| **Hybrid vehicle** | Fleet unit eligible for both chauffeured trips and self-drive rentals subject to maintenance and policy gates. |
| **Correlation ID** | End-to-end identifier attached to requests and logs to stitch gateway, services, and async consumers. |
| **p95 latency** | 95th percentile response time: 95% of requests complete at or below this duration under measured load. |
| **SLO** | Service level objective—a target for reliability or latency backed by error budgets and alerting. |
| **Idempotency key** | Client-supplied unique token ensuring retries do not create duplicate bookings or double charges. |
| **BFF** | Backend-for-frontend: a thin API layer shaped to a specific client (often admin web) to reduce chatty calls to many microservices. |
| **Dead letter topic (DLQ)** | Kafka topic (or queue) holding messages that failed processing beyond retry policy—ops replay after fixes. |
| **Database per service** | Each microservice owns its schema and storage; no cross-service SQL joins at runtime—only APIs or events bridge domains. |

---

## Usage tip for search demos

Queries like **“What is surge pricing?”** should land here; longer architectural context appears in `05-high-level-architecture.md` and `02-requirements.md`.
