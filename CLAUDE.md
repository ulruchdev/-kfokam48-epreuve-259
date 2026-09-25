# CLAUDE.md — KFOKAM48 Attendance & Peer Review Tracker

Assessed project: the Git history, TDD trace and docs are graded as much as the code.
Specs live in `docs/` — read only the section you need, never whole files by default:
- Rules `RGx`, requirements `EFx`, decisions `DEC-x`: `docs/CAHIER_DES_CHARGES.md` §4–§7
- Backlog (issue numbers, order): `docs/backlog.md`
- Schema mirror: `docs/diagrammes/D2-data-model.md` (must match Flyway migrations)
- API contract (frozen, additive changes only): `api/contrat.yaml`
- Git rules: `CONTRIBUTING.md`

## Non-negotiable rules

- **TDD strict**: a red test is committed BEFORE the implementation (`test(...)` commit, then
  `feat(...)`/`fix(...)` commit). Test names cite the rule: `should_<x>_when_<y>_RG3`.
- **English everywhere** in code, comments, commits, PRs, issues. French survives only in:
  API error messages (imposed), table/column names (client language, mirror of D2), `docs/`.
- **Contract**: the 5 imposed operations keep exact paths/verbs/statuses. Every error body is
  `{"code","message"}`. New codes only inside an already-imposed status. Contract changes =
  separate `docs(api):` commit stating the reason.
- **Layers**: controller → service → repository. No JPA entity in JSON (DTO records in
  `web/dto/Dto.java`). Business exceptions in `web/erreur/BusinessExceptions.java`.
- **Schema**: Flyway only, `ddl-auto: validate`. Never edit an applied migration — add `V<n>__*.sql`
  and update D2 in the same PR.
- **Clean code**: short one-thing methods, one source of truth per rule, explicit constructors.

## Git flow

- One user story (issue) = one branch = one PR merged into `main` (merge commit). Branches are
  stacked in backlog order and their PRs merged in that order. Never deliver in one push.
- One branch per issue: `feat/us-<n>-<slug>`, `fix/issue-<n>-<slug>`, `docs/<slug>`, `chore/<slug>`.
- Conventional Commits, atomic, body ends with `Closes #<n>` (or `Refs #<n>`).
- Commits are NOT signed: no GPG (`commit.gpgsign=false`) and no `Co-Authored-By` trailer. Never push to / force-push `main`.
- Milestones are empty commits with exact messages `[JALON] v0.1` / `[JALON] v1.0`, pushed at once.
- Only the main session commits on the backend stack. A subagent running in its own worktree
  (`isolation: worktree`, e.g. `frontend-implementer`) may commit on its own branch, same rules.

## Commands

```bash
cd backend && MAVEN_OPTS=-Xmx512m ./mvnw -o -q test-compile        # fast compile check
cd backend && MAVEN_OPTS=-Xmx512m ./mvnw -o test -Dtest=<Class>     # one test class
cd backend && MAVEN_OPTS=-Xmx512m ./mvnw -o verify                  # full (Testcontainers, Docker needed)
docker compose up -d --build                                        # whole app
```
`-Xmx512m` avoids the JVM out-of-memory crashes seen on this machine (`hs_err_pid*.log`).
Docker Desktop 29+: tests pin `DOCKER_HOST` to the Linux engine pipe and API 1.44 (see pom surefire).

## Skills

- `.claude/skills/frontend-design` (Anthropic, Apache-2.0): load before designing any screen.
- `.claude/skills/webapp-testing` (Anthropic, Apache-2.0): Playwright helpers to debug the running UI.
- Frontend rules: `frontend/CLAUDE.md`.

## Context economy

- Delegate test runs to the `test-runner` agent (returns failures only).
- Delegate contract/schema audits to `contract-auditor` (read-only).
- Independent issues can be implemented in parallel by `tdd-implementer` with `isolation: worktree`.
- After each step, `docs-keeper` updates `docs/JOURNAL.md` / `docs/backlog.md`.
