# KFOKAM48 — frontend

Registre de présence et de relecture par les pairs : le client React de
l'épreuve. Trois écrans (formateur, étudiant, relecteur) derrière un choix
d'identité en liste, sans authentification (EF10, Q1).

**Stack (F1):** React 19 + Vite + TypeScript strict for a fast, typed SPA;
React Router 7 for the four identity-gated routes; TanStack Query 5 for
server state (caching, retries, loading/error states) and zustand for the
one piece of client state that matters (the chosen identity); axios + zod
so every API response is validated against `api/contrat.yaml` at the
boundary; Vitest + Testing Library + MSW for fast TDD without a real
backend, and Playwright/Chromium for the end-to-end flow against
`docker compose up`.

## Requirements

- Node 24, npm 11
- Backend running on `:8080` (see `../backend`) for `npm run dev`, or the
  full stack via `docker compose up --build` from the repo root

## Commands

```bash
npm install
npm run dev            # Vite on :5173, proxies /api to http://localhost:8080
npm run lint            # ESLint (typescript-eslint, react-hooks, react-refresh)
npm run test:ci          # Vitest run (component tests, MSW-backed)
npm run build            # tsc -b && vite build
npm run e2e               # Playwright (Chromium), app up via docker compose
```

## Architecture

See `CLAUDE.md` for the full source layout and rules (one screen/user story
per branch, TDD, contract-first).
