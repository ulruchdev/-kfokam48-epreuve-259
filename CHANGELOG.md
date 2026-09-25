# Changelog

All notable changes, one line per merged pull request. Format: [Keep a Changelog](https://keepachangelog.com/).

## [1.0.0] — 2026-09-25

Step 3 (envelope) and step 4.

### Fixed
- Simultaneous attendances are all recorded: reviewer assignment serialized by a row lock, duplicate attendance → 409 (#59, PR #60)
- `npm run build` restored (import extension in the e2e spec) (#55, PR #66)

### Changed — step-3 change of need
- Analysis and contract updated first: RG5 revised, RG16, DEC-13/14, D1/D2/D4, contract v1.3 (#61, PR #65)
- Each exercise is reviewed by two distinct peers; retained grade = average, provisional with one review; V4 migration (#62, PR #67)
- Student and trainer screens show the retained grade and the provisional state; e2e flow with two reviewers (#63, PR #68)

### Removed (scope sacrificed, CDC §10)
- Review amendment (EF11): a rendered review is final (Q15, DEC-1 revised); code removal tracked by #64

## [0.1.0] — 2026-09-25

Step 2: Must stories, then the Should ones the screens depend on.

### Added
- Spring Boot backend, Flyway schema and demo data, `docker compose up` (#17, PR #23)
- Open a session with a 15-minute code (#5, PR #24) · reviewer assignment (#8, PR #25) · attendance by code with the RG3 lock (#6, PR #26)
- Manual attendance (#11, PR #28) · exercise submission (#7, PR #29) · session closure (#12, PR #30) · review (#9, PR #31) · dashboard (#10, PR #32)
- Student grades API (#15, PR #33) · amendment (#13, PR #34) · link replacement (#14, PR #35) · assigned reviews (#16, PR #36)
- Swagger UI with the frozen contract (#37, PR #42) · realistic demo seed V3 (#38, PR #43) · trainer attendance visible (#41, PR #44)
- React frontend: identity, trainer, student, reviewer screens, e2e (#4, #10, #15, #9, PR #48–#52) · landing page, role guards, shadcn/ui (#39, PR #53) · session settings (#40, PR #54)

### Fixed
- Every error returns its real HTTP status (DEC-9) (#21, PR #27)
- Dashboard average formatted, e2e independent from the database content (#55, PR #56)
