# D3 — Sequence: "mark attendance"

> Mandatory diagram. Nominal case plus the two required error cases
> (expired code → 410, already present → 409) and the anti-guessing lock (429, RG3).
> HTTP codes match `api/contrat.yaml` exactly; error bodies match the imposed
> format `{"code", "message"}`.

```mermaid
sequenceDiagram
    actor E as Student
    participant F as Frontend
    participant C as PresenceController
    participant S as PresenceService
    participant DB as PostgreSQL

    E->>F: types the presence code
    F->>C: POST /api/presences { code, etudiantId }
    C->>C: @Valid body (400 CHAMP_MANQUANT if invalid)
    C->>S: markAttendance(code, etudiantId)
    S->>DB: load student (etudiantId)
    alt unknown student
        S-->>C: EtudiantInconnuException
        C-->>F: 400 { code: "ETUDIANT_INCONNU", message: "..." }
    end
    S->>DB: find session by code
    alt unknown code
        S->>DB: increment failed attempts (etudiantId)
        S->>S: 5th failure? -> lock 2 minutes (RG3)
        S-->>C: CodeInconnuException | TooManyAttemptsException
        C-->>F: 400 { code: "CODE_INCONNU", message: "..." }
        or locked out (RG3)
        C-->>F: 429 { code: "TOO_MANY_ATTEMPTS", message: "..." }
    else code expired (RG1: 15 min after opening) or session ended (RG2)
        S-->>C: CodeExpireException
        C-->>F: 410 { code: "CODE_EXPIRE", message: "Le code de presence a expire." }
    else student already marked present
        S-->>C: DejaPresentException
        C-->>F: 409 { code: "DEJA_PRESENT", message: "..." }
    else nominal case
        S->>DB: INSERT presence (session_id, etudiant_id, source = ETUDIANT)
        S->>S: assign pending exercises a reviewer\n(new attendee = new eligible reviewer, RG6/RG15)
        S-->>C: Presence created
        C-->>F: 201 { id, sessionId, etudiantId, source: "ETUDIANT" }
        F-->>E: confirmation
    end
```

## Alignment with the contract

| Case | HTTP | `code` | Contract line |
|---|---|---|---|
| Missing/invalid field | 400 | `CHAMP_MANQUANT` | `POST /api/presences` 400 |
| Unknown code | 400 | `CODE_INCONNU` | `POST /api/presences` 400 |
| Unknown student | 400 | `ETUDIANT_INCONNU` | extension (400) |
| 5 wrong attempts | 429 | `TOO_MANY_ATTEMPTS` | extension (RG3, Q4) |
| Expired code / session ended | 410 | `CODE_EXPIRE` | `POST /api/presences` 410 |
| Already present | 409 | `DEJA_PRESENT` | `POST /api/presences` 409 |
| Success | 201 | — (body `id, sessionId, etudiantId, source`) | `POST /api/presences` 201 |

> Note: RG2 (session ended) and RG1 (code expired) share the imposed `410 CODE_EXPIRE`,
> because the contract only defines 410 for this operation and the client-visible
> outcome is identical: the code no longer works. Documented in CDC §7.
