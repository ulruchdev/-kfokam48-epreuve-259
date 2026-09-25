# KFOKAM48 Epreuve — Attendance & Peer Review Tracker

Final fullstack assessment project: course session attendance with presence codes
and peer review of student exercises, with a trainer dashboard.

**Frontend: React (Vite + TypeScript)** — largest ecosystem and the stack the candidate masters
best, so the time goes to the analysis and the Git discipline; `npm run build` passes (F1).

## Repository layout

```
/docs         CAHIER_DES_CHARGES.md, JOURNAL.md, diagrams/
/api          contrat.yaml (API contract)
/backend      Spring Boot (Java 17+, Maven, committed mvnw wrapper)
/frontend     React (Vite)
```

## Workflow

See [CONTRIBUTING.md](CONTRIBUTING.md): one branch per issue, one PR per branch,
conventional commits, three `[JALON]` milestone commits.

## Stack

| Layer    | Technology |
|----------|------------|
| Backend  | Java 21, Spring Boot, Maven, Flyway |
| Frontend | React 19, Vite, TypeScript, Tailwind v4 + shadcn/ui, TanStack Query, zustand, axios, zod |
| Database | PostgreSQL 17 |
| Runtime  | Docker Compose (`db` + `backend` + `frontend`) |

## Status

- [x] Repository setup (.gitignore, workflow, API contract)
- [x] Analysis & specification (cahier des charges, diagrams, backlog)
- [x] v0.1 — Must features (backend, three screens, demo data, e2e)
- [ ] Envelope (step 3)
- [ ] v1.0 — final version

## Running

```bash
git clone https://github.com/ulruchdev/kfokam48-epreuve-259.git && cd kfokam48-epreuve-259
docker compose up --build
```

| Service | URL |
|---|---|
| Application (landing page, pick a role) | http://localhost:5173 |
| API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |

Demo data is loaded by Flyway at startup: promotion « KFOKAM48 - Promotion 2026 », six
students, one closed past session with attendances (one added by the trainer), two graded
exercises and one review never rendered.

## Tests

```bash
cd backend && ./mvnw verify                 # unit + integration tests (Testcontainers, Docker needed)
cd frontend && npm ci && npm run test:ci    # Vitest + Testing Library + MSW
cd frontend && npx playwright install chromium
cd frontend && npm run e2e                  # Playwright Chromium, app up with docker compose
cd frontend && npm run e2e:headed           # same, visible browser, slowed down
scripts/api-smoke.sh                        # 50 curl checks of every endpoint and error case
```

## API documentation

| URL | Content |
|---|---|
| http://localhost:8080/swagger-ui.html | Swagger UI — pick **Contrat figé** (the frozen `api/contrat.yaml`, source of truth) or **Généré depuis le code** |
| http://localhost:8080/v3/api-docs | OpenAPI 3 document generated from the code |
| http://localhost:8080/contrat.yaml | The frozen contract as served by the backend |

Every error body is `{"code", "message"}` with its real HTTP status (DEC-9 in the CDC).

## End-to-end API check

```bash
scripts/api-smoke.sh                 # against http://localhost:8080 (docker compose up)
```

50 curl checks: every endpoint of the contract, nominal case and each error case with its
HTTP status and error code, plus the RG7 view (grade visible, reviewer never exposed).
