# Payment Processing — Implementation Plan

| | |
|---|---|
| Status | **Draft for approval** — nothing implemented yet |
| Date | 2026-10-24 |
| Source | Design interview (grill-me) between the project owner and Claude; every item in §3 is an explicit owner decision |
| Next step | Owner approves this plan, then Phase 1 starts with **test cases first** (see §13) |

## 1. Goal

Let staff collect payment for orders, including split bills, with a **mock card terminal** and cash, so that
orders can reach `COMPLETED`, tables are released, and the admin dashboard reports money actually collected.
The design deliberately mirrors a real payment integration (idempotency, reservation, outbox, terminal inquiry)
so that replacing the mock later only means replacing one adapter.

## 2. Scope

**In scope (v1)**
- CARD (mock terminal) and CASH payments, one order = one check, many payments per check
- Split bills: N-way even split and arbitrary amounts (partial payments / balance model)
- Per-payment tips
- Manager/Admin refunds (full or partial, reason required)
- On-screen receipts with browser print
- Takeout: separate screen, pay at pickup, no-show handling
- Transactional outbox + Kafka relay for payment events
- Dashboard revenue switched to cash-basis
- Backend test infrastructure (there are no backend tests today)

**Out of scope for v1 (tracked in §15, do not forget)**
- Item-level split bills ("guest A pays items a,b,c")
- Daily settlement / end-of-day close (**owner confirmed this must be added next**)
- Third-party delivery integration (Uber Eats / DoorDash / SkipTheDishes)
- Real payment gateway, email/SMS receipts, MOBILE_WALLET in the UI, tip-adjust-after-authorization

## 3. Decision log

| # | Decision |
|---|---|
| 1 | Mock terminal only (in-backend, `mock` Spring profile). No Stripe or external keys/webhooks. |
| 2 | Unit of payment is **one order**. No multi-order table tabs, no pre-pay before kitchen for dine-in. Fully paid ⇒ order `COMPLETED`; dine-in table `OCCUPIED → CLEANING`. |
| 3 | Balance model: `remaining = total − Σ(CAPTURED + in-flight)`. v1 supports N-way even split and arbitrary partial amounts. Cent remainder goes to the last payer. |
| 4 | Tip is per payment. `order.total` never includes tip; `orders.tip_amount = Σ captured tips`. |
| 5 | Methods: CARD (mock) + CASH. CASH records tendered amount and change. MOBILE_WALLET stays in the DB CHECK, not in the UI. |
| 6 | **Refund only**, MANAGER/ADMIN, per payment, full or partial, reason required. Order stays `COMPLETED`; re-charge = new order. (Void was dropped, see 17.) |
| 7 | Double-charge/overpay protection: client idempotency key + order row lock + **reservation** (PENDING row counts toward the balance before the terminal is called). |
| 8 | Mock terminal outcome by "magic cents" (see §8). Deterministic, `mock` profile only. |
| 9 | Checkout entry: `/checkout` ("Open Checks") + a "Checkout" button on OCCUPIED table cards, same payment screen. Delivery is not paid in the POS. |
| 10 | Takeout: **fire to kitchen first, pay at pickup** (phone orders cannot take card numbers). Separate takeout screen. |
| 11 | Takeout requires customer name + phone, gets a pickup number, READY-and-unpaid orders older than N minutes are flagged "Stale"; no auto-cancel; MANAGER/ADMIN cancel with a reason. |
| 12 | Dashboard "Today's Revenue" = Σ captured billed amount − refunds (tips excluded). New "Outstanding" and "Tips" cards. |
| 13 | Payment events go through the **transactional outbox** (Kafka must never be on the money path). |
| 14 | Outbox relay = polling scheduler, `FOR UPDATE SKIP LOCKED`, at-least-once, consumers dedupe on outbox `id`. |
| 15 | Receipts: on-screen + browser print, one per payment plus a combined order receipt. No email/SMS. The mock terminal returns the same *shape* as a real one (brand, last4, auth code) so the adapter can be swapped. |
| 16 | Events: `payment.captured/failed/refunded`, `order.completed/cancelled` → topic `payment-events`, key = `orderId`. v1 consumer = integration test only. |
| 17 | Card is a **single-step sale** (authorize + capture in one call). No user-facing Void; `VOIDED` stays in the DB CHECK, unused. |
| 18 | Terminal timeout = **inquire before expire**; never fail a payment by clock alone. |
| 19 | Tests: unit tests + Testcontainers (real Postgres) integration tests; EmbeddedKafka for the relay. **Test cases are written before the code, from this document, never from the implementation.** |

## 4. Current state (verified in the repository)

- `payments` already exists (V1): methods `CARD|CASH|MOBILE_WALLET`; statuses `PENDING|AUTHORIZED|CAPTURED|FAILED|REFUNDED|VOIDED`; `payment_intent_id` UNIQUE; `gateway_reference`; `gateway_response` JSONB; `idempotency_key` UNIQUE (V2). `order_id` is not unique, so several payments per order already work.
- `orders` has `subtotal`, `tax_amount` (5 %, `HALF_UP`, scale 2), `tip_amount` (never set today), `total`, `table_id`, `order_type` (`DINE_IN|TAKEOUT|DELIVERY`), `@Version`.
- **Nothing sets `COMPLETED` or `CANCELLED`.** There is no `payment` Java package.
- `outbox_events` exists (V2) but nothing writes or reads it. Existing Kafka events (`order-fired-events`, `item-status-events`) are published synchronously and a failure throws (HTTP 500).
- Roles: the JWT filter populates `ROLE_<ROLE>` authorities and `@EnableMethodSecurity` is on, but no `@PreAuthorize` is used anywhere; role gating exists only in the frontend `ProtectedRoute`. `@EnableScheduling` is already on.
- Table `OCCUPIED` is set by the frontend (`TablePicker` calls the table status endpoint), not by the order service.
- Dashboard revenue is `SUM(orders.total)` of non-draft, non-cancelled orders created today (booked, not collected).
- Backend has **no tests** (`src/test` missing). `spring-boot-starter-test` and `spring-kafka-test` are in the pom; Testcontainers is not. Frontend has 26 Vitest tests.

### Existing issues this work must fix

1. **`PATCH /api/orders/{id}/status` accepts any status with no validation.** Any authenticated user can set `COMPLETED`/`CANCELLED` and bypass payment. Phase 1 must reject `COMPLETED` and `CANCELLED` there; `COMPLETED` only comes from the payment service and `CANCELLED` only from the manager cancel endpoint.
2. Money endpoints need **server-side** role checks (`@PreAuthorize`); frontend-only gating is not acceptable for refunds and cancellations.
3. Multiple orders can exist on one table, so "release the table" must check for other open orders (§5.3).

## 5. Domain rules

### 5.1 Money

- `BigDecimal`, scale 2, `RoundingMode.HALF_UP`. Never `double`.
- `order.total = subtotal + tax` (unchanged). Tip is separate everywhere.
- `paid = Σ amount of CAPTURED payments` (billed part only, tips excluded).
- `reserved = Σ amount of PENDING and AUTHORIZED payments`.
- `remaining = total − paid − reserved`. A new payment is rejected when `amount > remaining` (409).
- Refunds do **not** change `remaining` and do not reopen the order (decision 6).

### 5.2 Even split

`splitEvenly(remaining, n)` returns `n` amounts that sum exactly to `remaining`; the first `n−1` are `floor(remaining / n, 2)` and the last takes the remainder. Example: 10.00 / 3 → 3.33, 3.33, 3.34. This is a UI helper; the server only validates `0 < amount ≤ remaining`.

### 5.3 State machines

Payment: `PENDING → CAPTURED | FAILED`; `CAPTURED → REFUNDED` (only when fully refunded; partial refunds keep `CAPTURED` and increase `refunded_amount`). `AUTHORIZED` is a transient internal state inside the terminal call. `VOIDED` unused.

Order (new transitions only): `FIRED..READY → COMPLETED` when `remaining == 0` and no PENDING payment exists. `any non-terminal → CANCELLED` via manager cancel only. `COMPLETED`/`CANCELLED` are terminal.

Table: when a DINE_IN order becomes `COMPLETED` or `CANCELLED` and the table has **no other order** in a non-terminal status, `OCCUPIED → CLEANING`. If other open orders remain, the table stays `OCCUPIED`.

## 6. Data model (new migrations, one per phase)

Latest existing migration is `V6`. Numbers below are indicative; each phase adds its own file.

```sql
-- P1: payments
ALTER TABLE payments
  ADD COLUMN tip_amount       DECIMAL(10,2) NOT NULL DEFAULT 0.00 CHECK (tip_amount >= 0),
  ADD COLUMN tendered_amount  DECIMAL(10,2),            -- CASH only
  ADD COLUMN change_given     DECIMAL(10,2),            -- CASH only
  ADD COLUMN refunded_amount  DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  ADD COLUMN failure_code     VARCHAR(30),              -- DECLINED, INSUFFICIENT_FUNDS, TIMEOUT
  ADD COLUMN created_by       UUID REFERENCES users(id),
  ADD COLUMN updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  ADD CONSTRAINT chk_payment_amount_positive CHECK (amount > 0);

-- P1: orders (cancel + takeout)
ALTER TABLE orders
  ADD COLUMN cancel_reason    VARCHAR(255),
  ADD COLUMN cancelled_by     UUID REFERENCES users(id),
  ADD COLUMN cancelled_at     TIMESTAMP,
  ADD COLUMN customer_name    VARCHAR(100),
  ADD COLUMN customer_phone   VARCHAR(30),
  ADD COLUMN pickup_number    VARCHAR(10),
  ADD COLUMN source           VARCHAR(10) CHECK (source IN ('COUNTER','PHONE'));

-- P2: card details returned by the terminal
ALTER TABLE payments
  ADD COLUMN card_brand VARCHAR(20),
  ADD COLUMN card_last4 CHAR(4),
  ADD COLUMN auth_code  VARCHAR(20),
  ADD COLUMN needs_review BOOLEAN NOT NULL DEFAULT FALSE;  -- terminal inquiry failed, manager must check

-- P3: outbox
ALTER TABLE outbox_events
  ADD COLUMN attempts   INT NOT NULL DEFAULT 0,
  ADD COLUMN last_error TEXT,
  ADD COLUMN topic      VARCHAR(100) NOT NULL DEFAULT 'payment-events';

-- P4: refunds
CREATE TABLE payment_refunds (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  payment_id UUID NOT NULL REFERENCES payments(id),
  amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
  reason VARCHAR(255) NOT NULL,
  refunded_by UUID NOT NULL REFERENCES users(id),
  idempotency_key VARCHAR(36) UNIQUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_refunds_payment ON payment_refunds(payment_id);
```

Never stored: full card numbers, CVV, anything the terminal does not return. Customer phone is stored for takeout only; a retention/deletion policy is decided together with daily settlement.

## 7. API

All endpoints require authentication. Role gating is enforced server-side with `@PreAuthorize`.

| Method & path | Roles | Purpose |
|---|---|---|
| `POST /api/orders/{orderId}/payments` | SERVER, MANAGER, ADMIN | Take a payment. Header `Idempotency-Key` (UUID, required). Body: `method` (`CARD`/`CASH`), `amount`, `tip` (default 0), `tendered` (CASH only). Returns the payment. |
| `GET /api/orders/{orderId}/payments` | same | Payments and balance (`total`, `paid`, `reserved`, `remaining`, `tipTotal`). |
| `GET /api/checks` | same | Open checks: orders in `FIRED…READY`, unpaid balance, filter `?type=DINE_IN\|TAKEOUT`, `?tableId=`. |
| `POST /api/payments/{id}/refunds` | MANAGER, ADMIN | Body `amount`, `reason`. Header `Idempotency-Key`. |
| `GET /api/payments/{id}/receipt` | SERVER, MANAGER, ADMIN | Receipt for one payment. |
| `GET /api/orders/{id}/receipt` | same | Combined receipt (only when `COMPLETED`). |
| `POST /api/orders/{id}/cancel` | MANAGER, ADMIN | Body `reason`. Rejected if any CAPTURED payment exists (refund first). |
| `GET /api/takeout` | SERVER, MANAGER, ADMIN | Takeout list grouped by status, `stale` flag, pickup number. |
| `PATCH /api/orders/{id}/status` | unchanged | **Now rejects `COMPLETED` and `CANCELLED`** (400/409). |

Errors reuse `BusinessRuleException` (409) and `ResourceNotFoundException` (404). Specific cases: amount > remaining (409), missing/duplicate-with-different-body idempotency key (409), order not payable (409), CASH `tendered < amount + tip` (409), card declined (402-style business result returned as a normal payment response with `status=FAILED`, not an HTTP error).

`OrderResponse` gains `paidAmount`, `remainingAmount`, `tipAmount`, `pickupNumber`, `customerName`.

## 8. Core flows

### 8.1 Taking a payment (CARD)

1. Validate request; look up `payments.idempotency_key`. If it exists and the body matches, **return the stored result** (replay). If it exists with a different body, 409.
2. `BEGIN`; `SELECT … FROM orders WHERE id = ? FOR UPDATE`.
3. Check the order is payable (`FIRED..READY`, not terminal). Compute `remaining` (§5.1). Reject if `amount > remaining`.
4. Insert payment `PENDING` with `amount`, `tip`, `idempotency_key`, `created_by`. `COMMIT` (lock released).
5. Call `PaymentTerminal.charge(...)` **outside any transaction** (timeout 30 s).
6. `BEGIN`; lock the order again; set the payment to `CAPTURED` (store `card_brand`, `card_last4`, `auth_code`, `gateway_reference`, `captured_at`) or `FAILED` (`failure_code`). If `remaining` is now 0 and nothing is PENDING ⇒ order `COMPLETED`, table rule §5.3, write outbox rows. `COMMIT`.
7. If step 5 returned success but step 6 fails, call the adapter's internal `release(gatewayReference)` and keep the payment PENDING for the cleanup job. If the terminal call itself timed out, leave the payment PENDING (§8.4).

### 8.2 CASH

Same as 8.1 but no terminal call: validate `tendered ≥ amount + tip`, compute `change_given = tendered − amount − tip`, insert `CAPTURED` directly inside the locked transaction.

### 8.3 Refund

`@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")`. Lock the payment row; require `status = CAPTURED` and `amount ≤ payment.amount + tip − refunded_amount`; insert `payment_refunds`; increase `refunded_amount`; set `REFUNDED` when fully refunded; call the terminal refund (mock: always succeeds); write `payment.refunded` outbox row. Order status is untouched.

### 8.4 Terminal timeout — inquire before expire

The mock rule `.03` (no response) leaves the payment `PENDING`. A scheduled job (every 10 s) selects `PENDING` payments older than 30 s and asks the terminal `inquire(idempotencyKey)`:
- "charged" ⇒ treat as step 6 success (`CAPTURED`, order completion logic).
- "not charged" ⇒ `FAILED` with `failure_code = TIMEOUT`.
- inquiry failed ⇒ stay `PENDING`, set a `needs_review` marker (logged + shown to managers); never auto-fail.

While `PENDING` the amount stays reserved, which prevents a second charge for the same money.

### 8.5 Mock terminal (`mock` profile)

| Cents of `amount + tip` | Result |
|---|---|
| `.01` | `DECLINED` |
| `.02` | `INSUFFICIENT_FUNDS` |
| `.03` | No response (timeout). `inquire` answers "not charged" by default; a test switch makes it answer "charged". |
| anything else | Approved, after a configurable delay (default 1–2 s). Returns `brand` (e.g. VISA), `last4`, `authCode`, `gatewayReference`. |

Real amounts ending in `.01/.02/.03` will fail on the mock; this is documented in the UI help text. The `PaymentTerminal` interface exposes `charge`, `inquire`, `release`, `refund`; only `MockPaymentTerminal` exists in v1.

### 8.6 Outbox relay

Every state change above writes an `outbox_events` row **in the same transaction**. `OutboxRelay` runs every ~1 s: `SELECT … WHERE status='PENDING' ORDER BY created_at LIMIT 50 FOR UPDATE SKIP LOCKED`, publishes to Kafka (key = `orderId`), marks `PROCESSED`; on failure increments `attempts`, stores `last_error`, and after 10 attempts marks `FAILED` and logs at ERROR. Delivery is at-least-once; the event id (outbox `id`) is in the payload for consumer-side dedupe. Kafka being down never affects a payment.

Events: `payment.captured`, `payment.failed`, `payment.refunded`, `order.completed`, `order.cancelled`. Payload: `eventId`, `eventType`, `occurredAt`, `orderId`, `paymentId?`, `amount`, `tip`, `method`, `cardBrand?`, `cardLast4?`. No other card data.

## 9. Takeout

- Created on a separate takeout screen (counter or phone, stored in `source`). **Name and phone are required.** A pickup number (`P-001`, daily sequence) is generated.
- The order is fired to the kitchen immediately; it is paid at pickup through the normal payment screen/API. Unpaid takeout also appears in Open Checks.
- `GET /api/takeout` flags `stale = true` for orders `READY` and unpaid for more than `takeout.stale-minutes` (default 45). No auto-cancel.
- MANAGER/ADMIN cancel with a required reason (`CANCELLED`, `order.cancelled` event); this is counted as no-show/waste later in daily settlement.

## 10. Frontend

| Screen | Notes |
|---|---|
| `/checkout` Open Checks | List of unpaid orders (table, type, total, remaining), filters, opens the payment screen. |
| Payment screen | Balance header; split helper (2/3/N-way, custom amount); tip (presets none/15/18/20 % of the billed part + custom — **default, confirm in review**); method; CASH tendered/change; result state with "checking…" when PENDING; receipt on success. Generates and reuses the idempotency key per attempt. |
| `/tables` | OCCUPIED cards get a "Checkout" button to the same screen. |
| Takeout screen | Create (name, phone required, source), list by status with Stale badge, pay at pickup, manager cancel. |
| Receipt view | Per-payment and combined, `@media print` CSS, no card number field anywhere. |
| Refund | Manager/Admin only, on the payment/receipt view; reason required. |
| `/admin` | Cash-basis Revenue, new Outstanding and Tips cards; Avg order value and Avg revenue/table use `COMPLETED` orders. The "Delivery" button in `OrderTypeSelector` is left as is until the delivery integration is designed. |

## 11. Security & compliance

- Server-side `@PreAuthorize` on refunds, cancel, and (via role list) all payment endpoints; the frontend gating is a convenience only.
- No full card numbers, CVV, or tracks are ever accepted, logged, stored, or put in events. The mock fabricates `last4`.
- Idempotency keys are required on money-moving POSTs. Logs must not contain request bodies of payment calls.
- Customer name/phone (takeout) are the only new personal data; retention policy is a settlement-phase task.

## 12. Timing / configuration

All defaults live in `application.yml` and are overridable: terminal timeout 30 s, PENDING sweep interval 10 s, outbox poll 1 s, outbox max attempts 10, takeout stale minutes 45, mock latency 1–2 s. "Today" for the dashboard and any day boundary uses the same `ZoneId.systemDefault()` as the existing stats service until daily settlement defines a business day.

## 13. Test strategy

**Rule: test cases are written first, from this document, independent of the implementation. Tests are never adjusted to fit the code; if a test looks wrong, go back to the decision and discuss it.**

- Unit tests (fast, no DB): balance math, even split with remainder, tip/tax rounding, cash change, mock terminal rules, status transition rules.
- Integration tests (`*IT`, Testcontainers Postgres + `spring-kafka-test` EmbeddedKafka; separate Maven profile so `./mvnw test` stays fast): concurrency, idempotency replay, outbox relay, timeout inquiry, role enforcement, migrations apply on a clean DB.
- Frontend: Vitest for the payment screen logic (split helper, idempotency key reuse, state display), plus a manual browser check per phase.

### Test case catalog (to be turned into test files first)

**P1 — foundation + cash**
1. Pay exactly `remaining` in cash ⇒ payment `CAPTURED`, order `COMPLETED`, `change_given` correct.
2. Pay less than `remaining` ⇒ order stays open, `remaining` decreases.
3. `amount > remaining` ⇒ 409, nothing inserted.
4. `amount ≤ 0` ⇒ 400.
5. `tendered < amount + tip` ⇒ 409.
6. Same `Idempotency-Key` + same body ⇒ same payment returned, only one row. Same key + different body ⇒ 409.
7. Missing `Idempotency-Key` ⇒ 400.
8. Two concurrent payments each for the full `remaining` on one order ⇒ exactly one succeeds (real Postgres).
9. Payment on `DRAFT`, `COMPLETED`, `CANCELLED` order ⇒ 409.
10. Dine-in last payment ⇒ table `OCCUPIED → CLEANING`; if another open order exists on the table ⇒ table stays `OCCUPIED`.
11. `PATCH /api/orders/{id}/status` with `COMPLETED` or `CANCELLED` ⇒ rejected.
12. Cancel: SERVER ⇒ 403; MANAGER with reason ⇒ `CANCELLED`; without reason ⇒ 400; with a CAPTURED payment ⇒ 409.
13. Tip stored per payment and summed into `orders.tip_amount`; tip never changes `total` or `remaining`.
14. Unauthenticated ⇒ 401 on every payment endpoint.

**P2 — card, split, timeout**
1. Cents `.01` ⇒ `FAILED/DECLINED`; `.02` ⇒ `FAILED/INSUFFICIENT_FUNDS`; other ⇒ `CAPTURED` with brand/last4/authCode.
2. A FAILED payment releases its reservation (`remaining` restored); a retry with a **new** key works.
3. While a payment is PENDING, a second payment cannot take that reserved amount.
4. `.03` ⇒ payment stays `PENDING`; after 30 s the sweeper calls `inquire`: "not charged" ⇒ `FAILED/TIMEOUT`; "charged" ⇒ `CAPTURED` and order completion runs; inquiry failure ⇒ stays PENDING and flagged.
5. 3-way split of 10.00 ⇒ 3.33 + 3.33 + 3.34, order completes on the third, never earlier.
6. Mixed split: one CARD + one CASH payment complete an order.
7. Card succeeded but the DB write fails ⇒ adapter `release` is called and no CAPTURED row exists.
8. Terminal is never called inside an open DB transaction/lock (verified by a concurrent read of the order while a slow terminal call runs).

**P3 — events**
1. Capturing a payment writes exactly one `payment.captured` outbox row in the same transaction; a rolled-back payment writes none.
2. Relay publishes `PENDING` rows to `payment-events` keyed by `orderId` and marks them `PROCESSED`.
3. Kafka unavailable ⇒ payment still succeeds; rows stay `PENDING`, `attempts` grows, and they are published once Kafka returns.
4. After max attempts ⇒ `FAILED` with `last_error`.
5. A row published twice (simulated crash before marking) is deduplicated by the test consumer via `eventId`.
6. Two relay instances never publish the same row concurrently (`SKIP LOCKED`).
7. Order of events for one order is preserved.
8. Payload contains no card data beyond brand/last4.

**P4 — refund + receipts**
1. Full refund ⇒ payment `REFUNDED`, `refunded_amount = amount + tip`, order still `COMPLETED`.
2. Partial refunds accumulate; over-refund ⇒ 409.
3. SERVER ⇒ 403; refund of non-CAPTURED payment ⇒ 409; missing reason ⇒ 400; replayed key ⇒ one refund.
4. `payment.refunded` event emitted.
5. Payment receipt: cash shows tendered/change, card shows brand/last4/authCode only, shows remaining balance.
6. Combined receipt only when `COMPLETED`; totals equal the sum of its payments.

**P5 — screens**
1. Open Checks lists only unpaid payable orders; paid ones disappear after completion.
2. Payment screen reuses one idempotency key across a retry of the same attempt and creates a new one for a new attempt.
3. Split helper amounts always sum to the remaining balance.
4. Takeout create rejects missing name/phone; pickup number increments daily.
5. Stale badge appears after the configured minutes for READY+unpaid only.
6. Manager-only controls are hidden for SERVER (and the server still returns 403).
7. Manual browser pass: dine-in full pay, 3-way split, decline then retry, takeout pay at pickup, refund, no-show cancel, light and dark mode.

**P6 — dashboard**
1. Revenue = captured billed amounts − refunds today; tips excluded.
2. Outstanding = Σ remaining of open orders; Tips = Σ captured tips.
3. A refund made today for yesterday's sale reduces today's revenue (documented behavior).
4. Hourly chart and top items keep working with the new numbers.

## 14. Phases

Each phase is its own branch and PR, and follows **test cases → owner review → failing tests → implementation → green**.

| Phase | Branch | Deliverables | Done when |
|---|---|---|---|
| **P1 Foundation + cash** | `feature/payments-foundation` | Testcontainers in pom and `*IT` profile; V7 migrations (§6 P1); `payment` package (entity, repository, service, controller); lock + idempotency + reservation; balance; CASH; order `COMPLETED`; table release rule; `PATCH status` lock-down; manager cancel endpoint; `@PreAuthorize`; `OrderResponse` balance fields | P1 test catalog green; API verified with `api-tests.http`/curl |
| **P2 Card + split + timeout** | `feature/payments-card` | `PaymentTerminal` + `MockPaymentTerminal` (`mock` profile); card columns; tips; PENDING sweeper with inquiry; split flows | P2 catalog green |
| **P3 Events** | `feature/payments-outbox` | Outbox writes in the payment transactions; `OutboxRelay`; `payment-events` topic; test consumer | P3 catalog green |
| **P4 Refund + receipts** | `feature/payments-refund-receipts` | `payment_refunds`; refund endpoint; receipt endpoints | P4 catalog green |
| **P5 Screens** | `feature/payments-ui` (may be split: checkout, takeout) | `/checkout`, payment screen, table-card button, takeout screen, receipt and refund views | P5 catalog green + browser pass |
| **P6 Dashboard** | `feature/payments-dashboard` | Cash-basis revenue, Outstanding/Tips cards | P6 catalog green |

Order is P1 → P2 → P3 → P4, then P5 and P6 (P6 only needs P1). Until P5 ships, backend behavior is verified through the API tests and integration tests. If the owner wants a payment screen sooner, a minimal cash-only screen can be pulled into P1 (P1 grows).

## 15. Deferred work (must be raised proactively)

1. **Daily settlement / end-of-day close (owner: must be added).** Per-day totals by method, tips, refunds, tax; cash drawer reconciliation (expected = cash captured − change given + opening float vs counted, over/short); who closed and when; lock the business day; check open/unpaid orders and takeout no-shows (waste) before close; phone-number retention/deletion policy; define "business day" and refund attribution (currently refund counted on the day it is made). Reuse `AdminStatsService` aggregation where possible.
2. **Item-level split bills.** Guest A pays items a,b,c, guest B pays d,e. Builds on the balance model: link table `payment ↔ order_item` (optionally with quantity share), per-item tax allocation with exact rounding to `tax_amount`, tip allocation, shared items (half a pizza), assignment UI.
3. **Third-party delivery (Uber Eats / DoorDash / SkipTheDishes).** Separate project: inbound order feed (start with a stub), platform orders are prepaid ⇒ new payment method `PLATFORM`/`EXTERNAL` (migration, current CHECK forbids it), platform name + external id, commissions/payouts vs settlement, KDS ingestion, menu sync, accept/reject and prep time; decide the future of `DELIVERY` and the POS Delivery button.
4. Real gateway swap: replace `MockPaymentTerminal` (async result/webhook, provider refunds, secrets).
5. Optional: reopen-on-full-refund, email/SMS receipts, realtime dashboard consumer, audit-log consumer, MOBILE_WALLET.

## 16. Risks and open points

- **Tip presets** are not decided; the plan assumes none/15/18/20 % + custom (review item).
- **Business day** is a calendar day in the server time zone until daily settlement exists; late-night restaurants may see orders split across days.
- **Stuck PENDING** after a crash is handled by the sweeper only if the terminal can answer `inquire`; with a real provider this depends on the provider API.
- **Existing behavior kept**: the frontend marks a table `OCCUPIED` on selection, so a table can be occupied without an order; this plan does not change that.
- **KDS** polls nothing and refreshes on load/WebSocket; completed orders are already `READY`, so no KDS change is needed.
- **Docker/CI**: integration tests need Docker; if CI is added later it must provide it.
- **Frontend toolchain** was just upgraded (PR #5); P5 builds on Tailwind 4 and React Router 7.
