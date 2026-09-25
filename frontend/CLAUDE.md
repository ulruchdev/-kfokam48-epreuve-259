# frontend/CLAUDE.md — React client of the Attendance & Peer Review Tracker

Read the root `CLAUDE.md` first (TDD, English, git flow). This file only adds frontend rules.

## Stack (declared in README — F1)

React 19 · Vite · TypeScript strict · React Router 7 · TanStack Query 5 (server state) ·
zustand (client state: chosen identity only) · axios (single HTTP client) · zod (runtime
validation of every API response) · Vitest + Testing Library + MSW (unit/component) ·
Playwright + Chromium (e2e against `docker compose up`).

## Architecture

```
src/
  api/          http.ts (axios instance, baseURL "/api", error interceptor -> ApiError)
                schemas.ts (zod mirrors of api/contrat.yaml)  ·  one file per resource:
                sessions.ts presences.ts exercises.ts reviews.ts dashboard.ts promotions.ts
  features/
    identity/   pick promotion then student (EF10) — no password (Q1)
    trainer/    open session + show code (EF1), dashboard (EF6), manual attendance (EF7), close (EF8)
    student/    mark attendance with code (EF2), submit/replace link (EF3/EF12), my grades (EF9)
    reviewer/   assigned reviews (EF13), render/amend grade + comment (EF5/EF11)
  stores/       identityStore.ts (zustand, persisted: promotionId, studentId, role)
  components/   shared UI: ErrorMessage (shows API `message`), Loading, Layout
  routes.tsx    "/" identity · "/formateur" · "/etudiant" · "/relecteur"
  test/         setup.ts, msw handlers/server
e2e/            Playwright specs (Chromium only)
```

Each feature folder holds its screen components, `queries.ts` (TanStack Query hooks calling
`src/api/*`) and co-located `*.test.tsx`.

## Rules

- F3: API calls only through `src/api/*`; every query/mutation shows loading and error
  states; the error text is the API `message` (French, from the contract). Never
  recompute the average or any business rule on the client.
- Parse every response with zod; a schema mismatch is a bug to report, not to silence.
- ENF1: the three student/reviewer screens work at 360 px width without horizontal scroll.
- Visual design: follow `.claude/skills/frontend-design/SKILL.md` — a classroom-register
  identity, not a templated dashboard. CSS is not graded: keep it small (CSS modules).
- Dev: Vite proxy `/api` -> `http://localhost:8080`. Docker: multi-stage image, nginx serves
  `dist/` and proxies `/api` to `backend:8080`.

## Commands

```bash
npm run dev            # Vite on 5173, backend must run on 8080
npm run lint
npm run test:ci        # vitest run
npm run build          # tsc -b && vite build
npm run e2e            # playwright test (chromium), app up via docker compose
```
