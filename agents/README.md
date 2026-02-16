# Development Team Agents

Specialized agent definitions for the Restaurant POS project. Each agent has a specific role, file boundaries, completion checkpoints, and handoff protocols.

## Agents

| Agent | File | Role | Owns |
|-------|------|------|------|
| PM Agent | `pm-agent.md` | Planning, coordination, bug routing | `docker/`, `agents/`, docs |
| Backend Dev | `backend-dev-agent.md` | Java/Spring Boot APIs, DB, Kafka | `backend/src/main/` |
| Frontend Dev | `frontend-dev-agent.md` | React components, state, UI | `frontend/src/` (non-test) |
| QA Test | `qa-test-agent.md` | Unit & component tests | `*/test/` files only |
| QA Integration | `qa-integration-agent.md` | Integration & E2E tests | `*/test/` files only |

## Shared Contract

`api-contract.md` is the single source of truth for API shapes (endpoints, DTOs, enums, WebSocket topics, Kafka topics). All agents reference it.

- **Backend Dev** updates it when endpoints/DTOs change
- **Frontend Dev** reads it before making API calls
- **QA agents** validate against it when writing tests
- **PM** verifies it's current before assigning downstream work

## Handoff Flow

```
PM assigns task
  → Dev agent does work
  → Dev runs completion checklist (compile/lint + tests)
  → Dev updates api-contract.md (if API changed)
  → Dev writes handoff note
  → PM routes to next agent (Frontend and/or QA)
  → QA runs tests, files bug reports if needed
  → PM routes bugs back to correct dev agent
```

## Completion Checkpoints

Every agent has a mandatory checklist they must run before marking work done:

| Agent | Gate 1 | Gate 2 | Gate 3 |
|-------|--------|--------|--------|
| Backend Dev | `./mvnw compile` | `./mvnw test` | Update contract |
| Frontend Dev | `npm run lint` | `npm run build` | Verify against contract |
| QA Test | Tests pass | Happy + error paths | Bug reports filed |
| QA Integration | Tests pass | Contract validated | Side effects verified |

## Bug Routing

QA agents document bugs using a standard format with severity, steps to reproduce, and a `Route to:` field. PM routes them:
- API/DB/Kafka/WebSocket bugs → Backend Dev
- UI/state/rendering bugs → Frontend Dev
- Cross-boundary bugs → Both with coordination notes
