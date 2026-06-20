# RideFlex — Requirements

This document captures **functional** requirements by primary actor and a concise **non-functional** profile used for architecture and SLO discussions. Numeric targets are **fictional baselines** for the onboarding demo corpus.

---

## Functional requirements

### Riders (cab booking)

- **R-FR-001**: Rider shall request a trip by specifying pickup, optional stops, and drop-off; system shall return fare estimate and ETA band before confirmation.
- **R-FR-002**: Rider shall confirm booking and receive assigned driver and vehicle summary within platform-defined timeouts.
- **R-FR-003**: Rider shall see live trip status (en route, arrived, in progress, completed) and share trip link with trusted contacts.
- **R-FR-004**: Rider shall rate trip and driver post-completion; optional tipping where market configuration allows.
- **R-FR-005**: Rider shall cancel per published cancellation policy; fees may apply based on driver assignment state and time window.

### Drivers

- **D-FR-001**: Driver shall go online/offline; system shall only offer trips when driver credentials and vehicle inspection are valid.
- **D-FR-002**: Driver shall accept or decline offered trips within configurable acceptance window without undue penalty for isolated declines (policy-driven).
- **D-FR-003**: Driver shall complete navigation-assisted trip flow with start/end timestamps captured for billing and disputes.
- **D-FR-004**: Driver shall access earnings summaries and payout schedule information (integration details out of scope for this demo).

### Car-rental customers

- **C-FR-001**: Customer shall search available vehicles by time window, location, class, and price.
- **C-FR-002**: Customer shall complete rental reservation with selected add-ons and acknowledge vehicle condition at pickup.
- **C-FR-003**: Customer shall extend or early-return rental subject to inventory and pricing rules.
- **C-FR-004**: Customer shall receive digital contract summary and return checklist.

### Fleet admins / operations

- **A-FR-001**: Admin shall onboard vehicles, assign them to ride pool, rental pool, or hybrid eligibility, and set maintenance holds.
- **A-FR-002**: Admin shall configure geofences, surge multipliers, and rental base rates by zone and time band.
- **A-FR-003**: Admin shall view operational alerts (e.g., prolonged idle, missed handoff, incident flags) and export audit-oriented reports.
- **A-FR-004**: Admin shall manage driver onboarding pipeline status (documents, training, activation).

### Cross-cutting

- **X-FR-001**: Authenticated users shall have a single identity with role-specific profiles (rider, driver, rental customer where applicable).
- **X-FR-002**: Payments shall support authorized capture, partial capture, refunds, and cancellation fees per product rules.
- **X-FR-003**: Notifications shall be delivered for booking lifecycle events across push, SMS, and email channels per user preferences.

---

## Non-functional requirements

### Availability and resilience

- **NFR-A-001**: Core booking APIs (create trip, get status, cancel) target **99.9%** monthly availability in primary region, excluding planned maintenance windows communicated in advance.
- **NFR-A-002**: Degraded mode shall allow **read-mostly** trip status and driver contact display when matching is slowed, with clear UX messaging.

### Latency (matching, pricing, search)

- **NFR-L-001**: **Driver matching** (request → first offer or “no drivers” response): **p95 under 3 seconds** in dense urban cells under nominal load.
- **NFR-L-002**: **Fare estimate** for rides: **p95 under 800 ms** when cache warm; fallback path may be slower but shall complete or fail explicitly.
- **NFR-L-003**: **Rental inventory search** first page: **p95 under 1.2 seconds** including availability filter.

### Data retention and privacy (demo policy)

- **NFR-D-001**: Trip and rental **billing records** retained **7 years** (fictional finance policy).
- **NFR-D-002**: **Location trace** for active trips retained **90 days** in queryable analytics store; coarse aggregates retained longer.
- **NFR-D-003**: **Support tickets and chat transcripts** retained **2 years** unless legal hold applies.
- **NFR-D-004**: **Right-to-erasure** requests processed per privacy program; some fields may be retained in anonymized form for analytics.

### Security and compliance (high level)

- **NFR-S-001**: TLS for all external traffic; encryption at rest for databases holding PII and payment tokens.
- **NFR-S-002**: Role-based access for admin tools; all sensitive actions auditable with actor, timestamp, and correlation ID.

---

## Traceability note

Requirements IDs are stable within this demo folder for search exercises (e.g., grep `R-FR-001`). They do not imply a real ticketing system.
