# RideFlex — Key end-to-end flows

The following **Mermaid sequence diagrams** describe representative interactions. Actors and steps are simplified for readability; retries, idempotency keys, and id_token propagation are omitted unless noted.

---

## 1. Ride booking flow (happy path)

```mermaid
sequenceDiagram
  autonumber
  participant R as Rider app
  participant G as API Gateway
  participant BT as Booking / Trip Service
  participant PR as Pricing Service
  participant DM as Driver Matching Service
  participant PY as Payment Service
  participant NS as Notification Service

  R->>G: POST /trips (pickup, dropoff)
  G->>BT: Authorized create trip
  BT->>PR: Request fare estimate + snapshot
  PR-->>BT: pricing_snapshot_id, estimate
  BT->>PY: Authorize hold (optional market)
  PY-->>BT: payment_intent_id, authorized
  BT->>DM: Request driver assignment
  DM-->>BT: driver_id, vehicle_id, eta_sec
  BT-->>G: 201 Created trip + assignment
  G-->>R: Trip confirmation
  BT->>NS: Emit TripBooked (async)
  NS-->>R: Push notification (driver found)
```

**Narrative**: The rider receives a fare anchored to a **pricing snapshot** before the trip is materially committed. Matching returns an assignable driver under nominal supply. Notification confirms asynchronously so a slow SMS provider does not block the HTTP response.

---

## 2. Car rental booking flow

```mermaid
sequenceDiagram
  autonumber
  participant C as Rental customer app
  participant G as API Gateway
  participant BT as Booking / Trip Service
  participant FV as Fleet and Vehicle Service
  participant PR as Pricing Service
  participant PY as Payment Service
  participant NS as Notification Service

  C->>G: GET /rentals/search (window, location)
  G->>FV: Query available vehicles
  FV-->>G: Vehicle options + lot metadata
  G-->>C: Search results
  C->>G: POST /rentals/reserve (vehicle_id, add-ons)
  G->>BT: Create rental reservation
  BT->>FV: Lock vehicle inventory slot
  FV-->>BT: lock_token, vehicle_id
  BT->>PR: Price rental package + snapshot
  PR-->>BT: pricing_snapshot_id, total
  BT->>PY: Authorization hold (deposit + rental)
  PY-->>BT: payment_intent_id
  BT-->>G: Reservation confirmed
  G-->>C: Confirmation + pickup code
  BT->>NS: Emit RentalReserved (async)
  NS-->>C: Email summary
```

**Narrative**: Inventory **locking** prevents double booking of the same physical unit. Deposit handling is modeled as payment authorization; capture timing follows rental lifecycle events not shown here.

---

## 3. Driver onboarding flow

```mermaid
sequenceDiagram
  autonumber
  participant D as Driver app
  participant G as API Gateway
  participant US as User Service
  participant FV as Fleet and Vehicle Service
  participant BT as Booking / Trip Service (readiness hook)
  participant NS as Notification Service

  D->>G: Start driver application
  G->>US: Create driver profile draft
  US-->>G: driver_application_id
  G-->>D: Continue onboarding wizard
  D->>G: Upload documents (presigned URLs fictional)
  G->>US: Attach document metadata + status pending
  US-->>G: OK
  D->>G: Register vehicle + inspection photos
  G->>FV: Create vehicle record + inspection pending
  FV-->>G: vehicle_id
  G->>US: Link driver to vehicle
  US-->>G: linkage stored
  Note over US,FV: Manual or automated verification (out of band)
  US->>NS: Emit DriverVerificationComplete (async)
  NS-->>D: Push you are cleared to go online
  D->>G: Toggle online
  G->>US: Set driver availability active
  US-->>G: OK
```

**Narrative**: User Service remains the **identity and eligibility** anchor; Fleet owns **inspection state** for the physical asset. Booking service may subscribe to activation events to warm caches—shown only as conceptual “readiness” in this diagram.

---

## 4. Payment and cancellation flow

This flow combines **rider-initiated cancellation** after assignment with payment side effects. Amounts are illustrative.

```mermaid
sequenceDiagram
  autonumber
  participant R as Rider app
  participant Dr as Driver app
  participant G as API Gateway
  participant BT as Booking / Trip Service
  participant DM as Driver Matching Service
  participant PY as Payment Service
  participant NS as Notification Service

  R->>G: DELETE /trips/{id} (cancel)
  G->>BT: Cancel trip with reason code
  BT->>DM: Release driver assignment if active
  DM-->>BT: Released / noop
  BT->>PY: Apply cancellation fee policy
  alt fee_applies
    PY-->>BT: partial_capture(fee_amount)
  else no_fee
    PY-->>BT: void_authorization
  end
  BT-->>G: Trip cancelled + fee summary
  G-->>R: Confirmation UI payload
  BT->>NS: Emit TripCancelled (async)
  NS-->>R: Push + email receipt
  BT->>NS: Emit DriverTripOfferRevoked (async)
  NS-->>Dr: Push driver trip removed
```

**Narrative**: Cancellation is a **coordination** problem: release constrained supply (driver offer) and align money movement with policy. Driver notification may be direct through Notification templates keyed by `trip_id` and `driver_id`.

---

## How to use these flows in onboarding

- Map each arrow to **observability** exercises: which `correlation_id` appears across BT, PY, and NS logs.
- Extend with **failure swimlanes** (matching timeout, payment decline) as a learner exercise—keep fictional outcomes explicit in any appended material.
