# D2 — Data model (ER diagram)

> This diagram is the exact mirror of the Flyway migrations (`backend/src/main/resources/db/migration`).
> Any schema change must update both, in the same PR. Crow's foot notation.

```mermaid
erDiagram
    PROMOTION ||--o{ ETUDIANT : "has 1..*"
    PROMOTION ||--o{ SESSION : "has 0..*"
    ETUDIANT ||--o{ PRESENCE : "marks 0..*"
    SESSION ||--o{ PRESENCE : "collects 0..*"
    ETUDIANT ||--o{ EXERCICE : "submits 0..*"
    SESSION ||--o{ EXERCICE : "receives 0..*"
    EXERCICE ||--o{ RELECTURE : "reviewed by 0..2"
    ETUDIANT ||--o{ RELECTURE : "reviews 0..* as reviewer"

    PROMOTION {
        bigint id PK
        varchar nom UK "not null"
    }
    ETUDIANT {
        bigint id PK
        bigint promotion_id FK "not null -> promotion.id"
        varchar nom "not null, UNIQUE (promotion_id, nom)"
        integer tentatives_code "not null default 0 (RG3: wrong codes in a row)"
        timestamptz bloque_jusqu_a "nullable (RG3: 2-minute lock end)"
    }
    SESSION {
        bigint id PK
        bigint promotion_id FK "not null -> promotion.id"
        varchar titre "not null"
        varchar code UK "not null, unique among open sessions"
        timestamptz ouverture_at "not null = now at creation"
        timestamptz expiration_at "not null = ouverture_at + 15 min (RG1)"
        timestamptz fin_at "not null = ouverture_at + dureeMinutes, default 120 (DEC-2)"
        varchar statut "not null default OUVERTE, CHECK in (OUVERTE, TERMINEE, CLOTUREE)"
    }
    PRESENCE {
        bigint id PK
        bigint session_id FK "not null -> session.id"
        bigint etudiant_id FK "not null -> etudiant.id"
        varchar source "not null CHECK in (ETUDIANT, FORMATEUR) (RG12)"
        timestamptz marque_a "not null"
    }
    EXERCICE {
        bigint id PK
        bigint session_id FK "not null -> session.id"
        bigint etudiant_id FK "not null -> etudiant.id"
        varchar lien "not null, valid URI"
        varchar statut "not null CHECK in (EN_ATTENTE_AFFECTATION, EN_ATTENTE_RELECTURE, RELU) (RG15, D4)"
        timestamptz depose_a "not null"
    }
    RELECTURE {
        bigint id PK
        bigint exercice_id FK "not null -> exercice.id"
        smallint rang "not null, CHECK in (1, 2), UNIQUE (exercice_id, rang) (RG5: two reviewers max)"
        bigint relecteur_id FK "not null -> etudiant.id, UNIQUE (exercice_id, relecteur_id) (RG4: != author)"
        integer note "nullable until submitted, CHECK 0-20 when not null (RG8)"
        text commentaire "nullable until submitted"
        boolean rendue "not null default false"
        timestamptz rendue_a "nullable"
        timestamptz maj_a "nullable, legacy of EF11 amendments (removed at step 3), kept by V4"
    }
```

## Invariants enforced beyond the schema

| Invariant | How | Rule |
|---|---|---|
| One presence per (session, student) | `UNIQUE (session_id, etudiant_id)` on PRESENCE | Q2/Q3 |
| One exercise per (session, student) | `UNIQUE (session_id, etudiant_id)` on EXERCICE → `409 EXERCICE_DEJA_DEPOSE` | contract |
| One student name per promotion | `UNIQUE (promotion_id, nom)` on ETUDIANT (identity picked by name, Q1) | EF10 |
| Code unique among open sessions | partial unique index `uniq_session_code_active` (`statut <> 'CLOTUREE'`) | DEC-5 |
| Two distinct reviewers per exercise, never three | `rang CHECK (1, 2)` + `UNIQUE (exercice_id, rang)` + `UNIQUE (exercice_id, relecteur_id)` on RELECTURE (V4) | RG5 revised (step 3) |
| Retained grade and provisional flag | computed by the service from the rendered reviews, never stored | RG16, DEC-13 |
| Reviewer ≠ author | service check → `403 AUTO_RELECTURE` | RG4 (Q5) |
| Reviewer among session attendees | service check at assignment | RG6 (Q7) |
| Attendance before submission | service check → `400 PRESENCE_REQUISE` | RG14 (DEC-4) |
| Rate limit on wrong codes | failed-attempt counter per student, 5 → 2 min lock | RG3 (Q4) |
| Link replaceable until review starts | service check → `409 RELECTURE_COMMENCEE` | RG11 (Q13) |
| Session closure freezes changes | status check on all write paths → `409 SESSION_CLOTUREE`; the code of a closed session → `410 CODE_EXPIRE` | RG9/RG10, DEC-11 |

> Integrity rules that PostgreSQL can enforce are enforced in the schema (UNIQUE, CHECK).
> Business rules that need context (rate limiting, attendance-before-submission,
> reviewer eligibility) live in the service layer and are covered by unit tests.

> `TERMINEE` is allowed by the CHECK constraint but never persisted: a session is "ended" when
> `now > fin_at` (RG2), computed at request time, so no scheduler has to flip the status. Only
> `OUVERTE → CLOTUREE` is written (EF8). V2 and V3 are data-only migrations (demo seed): D2 is
> unchanged by them.

> **Step 3 (V4, consequence of the two-reviewer change).** `UNIQUE (exercice_id)` is dropped,
> `rang` is added (existing reviews get rank 1), exercises with a single reviewer go back to
> `EN_ATTENTE_AFFECTATION` (DEC-14). V1–V3 are untouched.
