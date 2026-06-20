# RideFlex — Backend modules

RideFlex’s **logical backend** is decomposed into domain-oriented services. Each module below owns specific aggregates and publishes or consumes events through the platform message bus. Names and boundaries are **invented** for onboarding search and architecture exercises.

---

## User Service

**Responsibility**: Canonical identity, authentication tokens (integration with an IdP), rider/driver/rental profiles, consent flags, and device sessions. It owns **user_id** and profile attributes that other services must not duplicate as sources of truth.

**Talks to**: Payment Service (customer references), Notification Service (contact preferences), Fleet & Vehicle Management (driver–vehicle linkage metadata by reference only). Exposes REST/GraphQL to the API gateway; emits `UserUpdated`, `DriverActivated` events.

---

## Booking / Trip Service

**Responsibility**: Lifecycle of **chauffeured trips** and **rental reservations**: create, modify, cancel, complete; state machine enforcement; handoff timestamps; links to pricing snapshots and payment intents.

**Talks to**: Driver Matching (assignment requests and outcomes), Pricing (fare and rental quotes), Payment (authorize/capture/refund), Fleet (vehicle eligibility for rental), Notification (lifecycle emails/pushes). Heavy **async** coupling for post-booking side effects.

---

## Driver Matching Service

**Responsibility**: Geospatial candidate search, scoring, offer fan-out, acceptance tracking, and reassignment. Optimized for low latency and fairness constraints (fictional policy engine).

**Talks to**: User (driver capability flags), Fleet (vehicle class, inspection validity), Booking/Trip (demand signals), Pricing (optional surge context for driver display). Uses **Redis** for geo indices and ephemeral offer state; Kafka for `TripRequested`, `DriverAssigned` events.

---

## Pricing Service

**Responsibility**: Computes **ride estimates** and **final fares**, rental base rates with dynamic factors, surge multipliers, and tax/fee line items. Stores **versioned rules** and returns immutable **pricing snapshots** referenced by bookings.

**Talks to**: Booking (quote + snapshot attachment), Fleet (zone overlays), Admin configuration feeds. Read-heavy; cache-friendly.

---

## Payment Service

**Responsibility**: Tokenized payment instruments, authorization holds, captures, refunds, chargebacks workflow hooks, and reconciliation export stubs.

**Talks to**: Booking (amount milestones), User (billing identity), external PSP (fictional). Emits `PaymentCaptured`, `PaymentFailed` for downstream analytics and support.

---

## Fleet & Vehicle Management Service

**Responsibility**: Vehicles, inspections, maintenance windows, assignment to pools (ride/rental/hybrid), telematics ingestion adapters (non-specific), and geofence attachment to fleet objects.

**Talks to**: Matching (supply filters), Booking (rental inventory locks), Pricing (zone-based modifiers), Admin UI backends.

---

## Notification Service

**Responsibility**: Template rendering, channel selection, rate limiting, and delivery webhooks. Does not own deep business rules; consumes **facts** from other services.

**Talks to**: All major services via events; SMS/email/push providers (fictional). Correlates with `correlation_id` from gateway.

---

## Rating & Review Service

**Responsibility**: Post-trip and post-rental ratings, free-text reviews with moderation queues, aggregate scores for drivers and rental units.

**Talks to**: User (display names, anonymization), Booking (completed events only), Admin (moderation). Writes are **eventually consistent** in rider/driver UIs.

---

## Module interaction principle

**Synchronous** calls are reserved for user-facing paths that need an immediate answer (quotes, booking creation, payment authorization). **Asynchronous** events propagate truth for notifications, analytics, and non-critical denormalization. The API gateway remains **thin**: authn/authz, routing, rate limits, and request shaping only.
