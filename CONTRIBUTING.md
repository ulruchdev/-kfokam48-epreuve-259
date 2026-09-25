# Contributing — Git Workflow

This document describes the Git workflow used for this project. It is binding:
the repository history is part of the assessment.

## 1. Branching model

- `main` is always healthy. Every change goes through a branch and a Pull Request.
- **One branch per issue, one Pull Request per branch.** The words "ticket" and
  "issue" mean the same thing; this project uses **issue** only.

Branch naming:

| Type   | Pattern                        | Example                        |
|--------|--------------------------------|--------------------------------|
| Feature (user story) | `feat/us-<n>-<slug>` | `feat/us-01-open-session` |
| Fix    | `fix/issue-<n>-<slug>`         | `fix/issue-7-code-expiry`      |
| Docs   | `docs/<slug>`                  | `docs/cdc-v1`                  |
| Chore  | `chore/<slug>`                 | `chore/flyway-setup`           |

## 2. Issues

Yes, you must create issues — they ARE the backlog. One issue = one user-visible
result, described with:
- a title stating a outcome ("As a trainer, I open a session..."),
- acceptance criteria, verifiable, written as "When ... then ...";
- a priority: Must / Should / Could;
- a reference to the requirement (`EFx`) or business rule (`RGx`) it implements.

Example of a complete issue: see #5 or #6 on the repository — actor, path,
preconditions, field constraints, numbered business rules, nominal and alternative
scenarios, postconditions, acceptance criteria, Definition of Done.

Branches and commits reference the issue: the commit message ends with `Closes #4`
(the issue number), so the issue closes automatically when the PR merges.

## 3. Commits

Conventional Commits, atomic, one logical change per commit:

```
<type>(<scope>): <what it does, in English>

Closes #<issue>
```

Types: `feat`, `fix`, `docs`, `chore`, `test`, `refactor`.

Language: **all commit messages, PR titles, issues and code comments are in English.**

## 4. Milestone commits (assessment markers)

Three empty commits mark progress. They are created with the exact messages:

```
git commit --allow-empty -m "[JALON] analyse"
git commit --allow-empty -m "[JALON] v0.1"
git commit --allow-empty -m "[JALON] v1.0"
```

Rules:
- `[JALON] analyse` is pushed **before the first code commit** (order in history is what counts).
- Each milestone commit is pushed immediately. An unpushed milestone does not exist.
- Nothing else is attached to these commits. There are exactly **three** milestones.
- The repository-verification commit (from the LISEZ-MOI checklist) is a normal
  chore commit named `chore: verification du depot` — it **never** carries a
  `[JALON]` message, so it cannot be confused with a milestone.

## 5. Pull Requests

- PR title follows the conventional-commit format; the description lists the
  acceptance criteria checked.
- Every PR is linked to its issue.
- Merge into `main` with a merge commit (history keeps the branch structure).
- No direct push to `main`. No force-push on `main`, ever.

## 6. Repository hygiene

- `.gitignore` (Java + Node) is in place **before the first code commit**.
- Never commit: `target/`, `node_modules/`, `dist/`, IDE files, logs, `.env`.
- Never commit a secret. Configuration values come from `.env` (documented in `.env.example`).
- Push regularly — work that stays local is invisible to the reviewer.

## 7. API contract

`api/contrat.yaml` is **frozen before the first code commit**. Any later change
of the contract is a deliberate, documented decision (separate commit referencing
the reason).
