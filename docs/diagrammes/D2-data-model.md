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
    EXERCICE ||--o| RELECTURE : "reviewed by 0..1"
    ETUDIANT ||--o{ RELECTURE : "reviews 0..* as reviewer"

    PROMOTION {
        bigint id PK
        varchar nom UK "not null"
    }
    ETUDIANT {
        bigint id PK
        bigint promotion_id FK "not null -> promotion.id"
        varchar nom "not null"
    }
    SESSION {
        bigint id PK
        bigint promotion_id FK "not null -> promotion.id"
        varchar titre "not null"
        varchar code UK "not null, unique among open sessions"
        timestamptz ouverture_at "not null = now at creation"
        timestamptz expiration_at "not null = ouverture_at + 15 min (RG1)"
        timestamptz fin_at "not null = ouverture_at + dureeMinutes, default 120 (D2)"
        varchar statut "not null CHECK in (OUVERTE, TERMINEE, CLOTUREE)"
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
        bigint exercice_id FK "not null, UNIQUE -> exercice.id (RG5: one reviewer per exercise)"
        bigint relecteur_id FK "not null -> etudiant.id (RG4: != author)"
        integer note "nullable until submitted, CHECK 0-20 when not null (RG8)"
        text commentaire "nullable until submitted"
        boolean rendue "not null default false"
        timestamptz rendue_a "nullable"
        timestamptz maj_a "nullable, updated on amendment until closure (RG9)"
    }
```

## Invariants enforced beyond the schema

| Invariant | How | Rule |
|---|---|---|
| One presence per (session, student) | `UNIQUE (session_id, etudiant_id)` on PRESENCE | Q2/Q3 |
| One review per exercise | `UNIQUE (exercice_id)` on RELECTURE | RG5 (Q6) |
| Reviewer ≠ author | service check → `403 AUTO_RELECTURE` | RG4 (Q5) |
| Reviewer among session attendees | service check at assignment | RG6 (Q7) |
| Attendance before submission | service check → `400 PRESENCE_REQUISE` | RG14 (D4/H4) |
| Rate limit on wrong codes | failed-attempt counter per student, 5 → 2 min lock | RG3 (Q4) |
| Link replaceable until review starts | service check → `409 RELECTURE_COMMENCEE` | RG11 (Q13) |
| Session closure freezes changes | status check on all write paths | RG9/RG10 |

> Integrity rules that PostgreSQL can enforce are enforced in the schema (UNIQUE, CHECK).
> Business rules that need context (rate limiting, attendance-before-submission,
> reviewer eligibility) live in the service layer and are covered by unit tests.
