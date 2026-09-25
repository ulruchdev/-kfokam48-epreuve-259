# D4 — State machine: exercise lifecycle (BONUS diagram)

> Bonus diagram (+3). **Revised at step 3** (two reviewers per exercise, EF11 removed). States match the `statut` CHECK constraint of the EXERCICE
> table in D2 and the `statut` enum of `/api/exercices` in the contract.

```mermaid
stateDiagram-v2
    [*] --> EN_ATTENTE_AFFECTATION : POST /api/exercices, fewer than two eligible reviewers (RG15)
    [*] --> EN_ATTENTE_RELECTURE : POST /api/exercices, two distinct reviewers drawn (RG5, RG6)

    EN_ATTENTE_AFFECTATION --> EN_ATTENTE_AFFECTATION : new attendance, first reviewer drawn, or its review rendered (grade provisional, RG16)
    EN_ATTENTE_AFFECTATION --> EN_ATTENTE_RELECTURE : new attendance, second reviewer drawn (RG15)

    EN_ATTENTE_RELECTURE --> EN_ATTENTE_RELECTURE : first review rendered, grade provisional (RG16)
    EN_ATTENTE_RELECTURE --> RELU : second review rendered, retained grade = average (RG16)

    EN_ATTENTE_AFFECTATION --> [*] : session closed, grade stays provisional or null (Q11)
    EN_ATTENTE_RELECTURE --> [*] : session closed, missing review counted as pending (Q11, Q16)

    note right of EN_ATTENTE_AFFECTATION
        Link replaceable as long as
        no review has been rendered:
        PUT /api/exercices/{id}
        -> 409 RELECTURE_COMMENCEE otherwise (RG11, Q13)
    end note

    note right of RELU
        Student sees the retained grade and
        both comments, never who reviewed (RG7, Q8).
        A rendered review is final (RG9, Q15)
    end note
```

## State transitions table

| From | Event | To | Guard / side effect |
|---|---|---|---|
| — | `POST /api/exercices` (attendance OK, RG14) | `EN_ATTENTE_AFFECTATION` or `EN_ATTENTE_RELECTURE` | up to two reviewers drawn at once; `EN_ATTENTE_RELECTURE` only when two are assigned |
| `EN_ATTENTE_AFFECTATION` | new `POST /api/presences` in the session | `EN_ATTENTE_AFFECTATION` or `EN_ATTENTE_RELECTURE` | missing reviewer(s) drawn among attendees ≠ author and ≠ current reviewer; `EN_ATTENTE_RELECTURE` once two are assigned (RG5, RG15) |
| `EN_ATTENTE_RELECTURE` | first `POST /api/relectures/{id}` | `EN_ATTENTE_RELECTURE` | grade shown, marked provisional (RG16) |
| `EN_ATTENTE_RELECTURE` | second `POST /api/relectures/{id}` | `RELU` | retained grade = average of the two (RG16) |
| any | trainer closes session (EF8) | terminal | no more submissions nor reviews (RG9, RG10) |
