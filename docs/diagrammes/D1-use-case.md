# D1 — Use case diagram

> Actors on the left, system boundary in the middle, automated system rules on the right.
> The "Reviewer" is NOT a distinct actor: it is a student in a transient role (see CDC §2).

```mermaid
flowchart LR
    FORMATEUR(["Actor: Trainer"])
    ETUDIANT(["Actor: Student"])
    SYSTEME(["System rules (automated)"])

    subgraph SYSTEM["Attendance & Peer Review Tracker"]
        UC1(("UC1 Open a session, get a presence code - EF1"))
        UC2(("UC2 Mark attendance with the code - EF2"))
        UC3(("UC3 Submit exercise link - EF3"))
        UC4(("UC4 Assign reviewer randomly among attendees - EF4"))
        UC5(("UC5 Submit review grade 0-20 and comment - EF5"))
        UC6(("UC6 View dashboard attendance, submissions, average, pending - EF6"))
        UC7(("UC7 Add manual attendance marked by trainer - EF7"))
        UC8(("UC8 Close the session - EF8"))
        UC9(("UC9 View own grades and comments, anonymous reviewer - EF9"))
        UC10(("UC10 Pick identity from student list - EF10"))
        UC11(("UC11 Amend own review until closure - EF11"))
        UC12(("UC12 Replace exercise link before review starts - EF12"))
        UC13(("UC13 List assigned reviews - EF13"))
    end

    FORMATEUR --> UC1
    FORMATEUR --> UC6
    FORMATEUR --> UC7
    FORMATEUR --> UC8

    ETUDIANT --> UC10
    ETUDIANT --> UC2
    ETUDIANT --> UC3
    ETUDIANT --> UC9
    ETUDIANT -->|"as reviewer"| UC5
    ETUDIANT -->|"as reviewer"| UC11
    ETUDIANT -->|"as reviewer"| UC13
    ETUDIANT -->|"own submission"| UC12

    SYSTEME -.->|"triggers"| UC4
```

## Notes

- **Reviewer = student in a state**: no separate actor, because a reviewer keeps all
  student capabilities and the data model stores the reviewer as a student (RG4, RG6).
- **System rules (right)**: reviewer assignment (Q7) is system-automated, hence the
  dashed trigger from the automated-rules actor to UC4.
- Extension points: UC2 includes rate limiting after 5 failed attempts (RG3, → 400 TOO_MANY_ATTEMPTS);
  UC3 requires prior attendance (RG14, → 400 PRESENCE_REQUISE).
