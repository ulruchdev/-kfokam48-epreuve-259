# D4 — State machine: exercise lifecycle (BONUS diagram)

> Bonus diagram (+3). States match the `statut` CHECK constraint of the EXERCICE
> table in D2 and the `statut` enum of `/api/exercices` in the contract.

```mermaid
stateDiagram-v2
    [*] --> EN_ATTENTE_AFFECTATION : POST /api/exercices, no eligible reviewer yet (RG15)
    [*] --> EN_ATTENTE_RELECTURE : POST /api/exercices, reviewer auto-assigned (RG6, Q7)

    EN_ATTENTE_AFFECTATION --> EN_ATTENTE_RELECTURE : new attendance in session, random draw (RG6 + RG15)

    EN_ATTENTE_RELECTURE --> RELU : POST /api/relectures, note 0-20 (RG8)
    RELU --> RELU : PUT amendment until closure (RG9, Q10)

    EN_ATTENTE_AFFECTATION --> [*] : session closed, exercise left pending, visible in dashboard (Q11)
    EN_ATTENTE_RELECTURE --> [*] : session closed, review never rendered, counted as pending (Q11, Q16)

    note right of EN_ATTENTE_AFFECTATION
        Link replaceable in any state
        as long as no review has started:
        PUT /api/exercices/{id}
        -> 409 RELECTURE_COMMENCEE otherwise (RG11, Q13)
    end note

    note right of RELU
        Student sees note + comment,
        never the reviewer name (RG7, Q8)
    end note
```

## State transitions table

| From | Event | To | Guard / side effect |
|---|---|---|---|
| — | `POST /api/exercices` (attendance OK, RG14) | `EN_ATTENTE_AFFECTATION` or `EN_ATTENTE_RELECTURE` | if another eligible attendee exists → draw reviewer immediately; else pending assignment |
| `EN_ATTENTE_AFFECTATION` | new `POST /api/presences` in the session | `EN_ATTENTE_RELECTURE` | random draw among attendees ≠ author (RG6, RG15) |
| `EN_ATTENTE_RELECTURE` | `POST /api/relectures/{id}` by assignee | `RELU` | note 0–20 integer (RG8) → exercise becomes reviewed |
| `RELU` | `PUT /api/relectures/{id}` by assignee | `RELU` | allowed until session closure (RG9, D1) |
| any | trainer closes session (EF8) | terminal | no more submissions (RG10), no more amendments (RG9) |
