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

## Running (planned)

Docker Compose startup instructions will be documented and tested from a clean
clone before the v1.0 milestone.
