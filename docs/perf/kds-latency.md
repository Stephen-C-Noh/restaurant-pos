# Fire → kitchen screen latency

How long after an order is fired does it show up on the kitchen display (KDS)? Measured, not estimated.

## Result

Local Docker Compose stack, one machine, idle, warm (after a discarded warm-up run). N = 20 per row.

| Date | Commit | Kitchen push path | Transport median / worst | On screen median / worst |
|---|---|---|---|---|
| 2026-11-17 | `2709bca` + probe | Direct WebSocket push after commit (before Phase 3) | 7 ms / 9 ms | 16 ms / 24 ms |
| 2026-11-17 | `f75853a` | Outbox → relay → Kafka → KDS consumer → WebSocket (Phases 3-4) | 14 ms / 20 ms | 22.5 ms / 35 ms |

Routing the push through the outbox and Kafka costs about 7 ms at the median. The worst case seen is 35 ms from fire to the card on screen.

These numbers cover the healthy, warm path only:
- **Cold start:** the first order after a backend restart took 84 ms to arrive (warm-up run, discarded).
- **Kafka outage:** an order fired while Kafka is down is not lost. It stays in the outbox and appears once Kafka is back. If the backend also restarted during the outage, the first push came about 50 s after Kafka returned (Phase 4 manual check). That delay is not part of the table.
- Everything ran on one host. A real restaurant LAN adds network time between the backend and the kitchen screen.

## What is timed

- `firedAt`: the order's `firedAt` from the `/topic/orders` payload. The server sets it inside the fire transaction, so the commit is included.
- `recvAt`: browser `Date.now()` at the top of the STOMP handler in `KitchenDisplay.jsx`.
- `renderAt`: browser `Date.now()` in the second `requestAnimationFrame` after the message, i.e. the frame after the state update was painted.

**Transport** = `recvAt - firedAt`. **On screen** = `renderAt - firedAt` (what the cook sees).

The probe is active only with `?latency=1` in the KDS URL and appends `{orderNumber, firedAt, recvAt, renderAt}` to `window.__kdsLatency`. Without the flag it does nothing (covered by a Vitest test).

## Clock

`firedAt` comes from the backend container's clock and the other two from the browser's, so both must share one clock. Docker Desktop runs the containers inside the WSL2 VM. A browser on Windows uses a different clock, which can drift. For these runs the browser was headless Chrome inside WSL. WSL and the containers report the same kernel `boot_id`, so they read the same clock and the skew is zero.

To check on another machine, compare `cat /proc/sys/kernel/random/boot_id` in the browser's environment and in `docker exec pos-backend`. If they differ, compare `date +%s%3N` on both sides and abort if they differ by more than ~50 ms.

## Procedure

1. `cd docker && docker compose up -d --build`, wait for `curl localhost:8090/actuator/health` to report `UP`.
2. In a browser in the same clock domain, log in as kitchen1 (PIN 7890) and open `http://localhost:5173/kds?latency=1`.
3. Warm-up: `scripts/fire-orders.sh 5`, then discard it with `window.__kdsLatency = []` in the console.
4. `scripts/fire-orders.sh 20` (logs in as server1, creates and fires 20 one-item takeout orders, 3 s apart).
5. In the console: `copy(JSON.stringify(window.__kdsLatency))`, then paste into the raw samples below and compute median and worst.

For the direct-push row, the probe diff was applied to `2709bca` (the merge before Phase 3) and both containers were rebuilt from that tree. The database was already at V10; Flyway ignores the newer migrations.

## Raw samples (ms after `firedAt`)

<details>
<summary>Direct push, 2709bca + probe</summary>

| Order | Transport | On screen |
|---|---|---|
| ORD-1117-052 | 9 | 20 |
| ORD-1117-053 | 8 | 13 |
| ORD-1117-054 | 7 | 16 |
| ORD-1117-055 | 7 | 12 |
| ORD-1117-056 | 8 | 21 |
| ORD-1117-057 | 6 | 17 |
| ORD-1117-058 | 6 | 16 |
| ORD-1117-059 | 8 | 14 |
| ORD-1117-060 | 6 | 13 |
| ORD-1117-061 | 7 | 13 |
| ORD-1117-062 | 8 | 12 |
| ORD-1117-063 | 6 | 23 |
| ORD-1117-064 | 7 | 22 |
| ORD-1117-065 | 8 | 15 |
| ORD-1117-066 | 7 | 13 |
| ORD-1117-067 | 8 | 24 |
| ORD-1117-068 | 8 | 16 |
| ORD-1117-069 | 6 | 23 |
| ORD-1117-070 | 7 | 21 |
| ORD-1117-071 | 6 | 19 |

</details>

<details>
<summary>Outbox + Kafka consumer, f75853a</summary>

| Order | Transport | On screen |
|---|---|---|
| ORD-1117-027 | 18 | 33 |
| ORD-1117-028 | 15 | 28 |
| ORD-1117-029 | 14 | 23 |
| ORD-1117-030 | 18 | 22 |
| ORD-1117-031 | 13 | 19 |
| ORD-1117-032 | 15 | 18 |
| ORD-1117-033 | 14 | 16 |
| ORD-1117-034 | 14 | 22 |
| ORD-1117-035 | 13 | 22 |
| ORD-1117-036 | 15 | 18 |
| ORD-1117-037 | 16 | 29 |
| ORD-1117-038 | 13 | 26 |
| ORD-1117-039 | 20 | 35 |
| ORD-1117-040 | 13 | 29 |
| ORD-1117-041 | 14 | 30 |
| ORD-1117-042 | 12 | 17 |
| ORD-1117-043 | 13 | 28 |
| ORD-1117-044 | 12 | 15 |
| ORD-1117-045 | 12 | 17 |
| ORD-1117-046 | 14 | 30 |

</details>
