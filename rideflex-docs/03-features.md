# RideFlex — Feature catalog

Features are grouped by **primary client surface** and user type. Wording mirrors how product and engineering might slice releases; nothing here implies a real roadmap or shipped product.

---

## Rider app features

- **Trip request & editing**: Pickup/drop-off pins, saved places, multi-stop trips, scheduled rides for supported markets.
- **Fare transparency**: Upfront estimate, surge indicator, breakdown of fees (platform, tolls, airport surcharges where applicable).
- **Driver matching UX**: Spinner with honest sub-states (“finding nearby drivers”, “confirming driver”), re-offer after decline.
- **In-trip experience**: Live map, ETA updates, one-tap safety tools, in-app chat with masked phone numbers.
- **Post-trip**: Digital receipt, rating and feedback, lost-item flow, support entry points.
- **Account & wallet**: Payment methods, promo codes, ride history, corporate profile stub (B2B demo optional).

---

## Driver app features

- **Online session management**: Heat maps (simplified), break mode, auto-offline on prolonged idle.
- **Trip inbox**: Incoming offers with earnings preview, accept/decline, timeout handling.
- **Navigation handoff**: Deep link to preferred navigation app; return to RideFlex for status transitions.
- **Earnings & incentives**: Daily/weekly summaries, quest-style incentives (fictional), tax document placeholder.
- **Quality & compliance**: Document expiry reminders, vehicle photo checklists, incident reporting wizard.

---

## Car rental features (customer-facing)

- **Discovery**: Filters by body style, transmission, seats, fuel type, and price; map of pickup lots and partner garages.
- **Booking wizard**: Date/time picker with minimum duration, mileage package selection, young-driver fee rules (configurable).
- **Pickup & return**: QR or code-based handoff, guided damage walkaround, fuel/charge level capture.
- **During rental**: Lock/unlock integration (fictional smart-fleet module), roadside assistance request, extension flow.
- **Closure**: Final invoice, deposit release timing copy, rebooking CTA.

---

## Admin / ops features

- **Fleet console**: Vehicle lifecycle (active, maintenance, retired), assignment to ride vs rental vs hybrid.
- **Pricing & zones**: Geofence editor, surge schedules, rental dynamic pricing knobs (seasonality factors).
- **Workforce ops**: Driver pipeline, fraud flags queue, manual trip adjustments with audit trail.
- **Command center lite**: City-level demand vs supply chart, alert feed (matching backlog, payment failures spike).
- **Content & policy**: In-app copy versions, cancellation policy matrix by product and region (demo tables only).

---

## Shared platform features (surface-agnostic)

- **Identity & consent**: Login, MFA for admin, marketing opt-in/out.
- **Notifications hub**: Template management, channel routing, quiet hours.
- **Payments orchestration**: Authorization holds for rentals, instant capture for completed rides, refund workflows.
- **Ratings & reviews**: Aggregation, abuse detection hooks, appeal workflow stubs.

---

## Out of scope (explicit for demo docs)

- Real-time public transit integration, micromobility, food delivery, and autonomous vehicle dispatch are **not** part of RideFlex v1 in this corpus. Mentioning them in search tests should return this section for disambiguation.

---

## Release grouping (illustrative, not a roadmap)

Product teams often bucket capabilities into **MVP**, **growth**, and **platform hardening** tranches even when dates are fake. For search corpora, the following mapping helps joiners practice traceability from feature bullets to service ownership:

- **MVP slice**: Core ride request/match/complete, basic rental search and checkout, payment authorization and capture, minimal admin fleet list, email receipts.
- **Growth slice**: Scheduled rides, rental extensions, incentive campaigns for drivers, richer admin geofencing, in-app chat moderation hooks.
- **Hardening slice**: Fraud queues, chaos drills for Kafka lag, finer-grained SLO dashboards, disaster recovery runbooks (described elsewhere if extended).

No commitment, staffing, or timeline is implied; the list exists to generate **cross-file keywords** (`MVP`, `growth`, `hardening`) for retrieval evaluation.
