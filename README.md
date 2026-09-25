# KFOKAM48 Epreuve — Attendance & Peer Review Tracker

Final fullstack assessment project: course session attendance with presence codes
and peer review of student exercises, with a trainer dashboard.

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
| Frontend | React 19, Vite, TypeScript |
| Database | PostgreSQL 17 |
| Runtime  | Docker Compose (`db` + `backend` + `frontend`) |

## Status

- [x] Repository setup (.gitignore, workflow, API contract)
- [ ] Analysis & specification (cahier des charges, diagrams, backlog)
- [ ] v0.1 — Must features
- [ ] Envelope (step 3)
- [ ] v1.0 — final version

## Running

```bash
docker compose up --build      # db (PostgreSQL 17) + backend on http://localhost:8080
```

Demo data is loaded by Flyway at startup (promotion 1, six students).

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
