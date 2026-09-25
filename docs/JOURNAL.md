# Journal de bord — TASSE TUEKAM Ulruch Baudrel (matricule 259)

> One entry per step, written **when the step is completed**.
> Each entry answers: what was done, what blocked me (and how long),
> what I asked the AI and **how I verified its answer**.

---

## Étape 1 — Analyse et conception

**Fait :** read the full subject and CLIENT.md (16 answers); produced the cahier des
charges with the 10 imposed sections — 13 functional requirements (EF1–EF13, 10 Must,
3 Should), 15 business rules (RG1–RG15) each sourced from a `Qx` or a documented
decision `DEC-x`; the Q10 vs Q15 contradiction resolved in favour of Q10; three blind
spots surfaced and closed (end-of-session definition DEC-2, sole-attendee reviewer
assignment DEC-3, missing promotions/students endpoints in the imposed contract);
4 Mermaid diagrams in `docs/diagrammes/`; API contract completed (5 imposed operations
untouched, 12 free endpoints, additive extensions only) and frozen before any code
commit; commit `[JALON] analyse` pushed.

**Bloqué :** ~25 min on the H4 decision (attendance required to submit an exercise?):
CLIENT.md is silent, Q16 suggests two independent counters; settled it as the client
would (attendance required, RG14, `400 PRESENCE_REQUISE`) and documented the trade-off
in CDC §7. ~10 min realizing the default branch of the new GitHub repo pointed at the
bootstrap branch — fixed via `gh repo edit --default-branch main`.

**IA :** used the AI assistant for the requirement/rule numbering proposal, the first
draft of the 4 diagrams, and the completion of the OpenAPI contract. **Verification:**
re-read every RG against its Qx source in CLIENT.md one by one; checked each contract
extension is strictly additive (no imposed path/verb/status removed — grep on the 5
imposed operations); validated `api/contrat.yaml` with `npx js-yaml` (parse OK);
cross-checked D3's HTTP codes against the contract table line by line; rejected the
AI's first draft of DEC-4 (it recommended "no attendance required") in favour of my
own decision, documented as such.

**Relecture qualité avant tout codage (post-jalon, toujours étape 1) :** 3 anomalies
détectées et corrigées — (1) l'exemple d'erreur du contrat avait perdu ses accents
imposés (`a expiré`), réécrit à l'identique du sujet ; (2) références ambiguës `D4/H4`
renommees `DEC-4` ; (3) RG3 utilisait un statut `429` ajouté sur une opération imposée
— ramené sous le statut 400 déjà imposé avec le code distinct `TOO_MANY_ATTEMPTS`
(B2 : statuts imposés intacts), corrigé dans le contrat, D1, D3, le CDC et l'issue #6.
Diff des statuts des 5 opérations imposées vérifié contre l'original : identiques.

---

## Étape 2 — Première version

**Fait :** backend complet livré issue par issue (une branche et une PR mergée par US, test rouge
commité avant chaque implémentation) : #17 socle + Docker, #5 sessions, #8 tirage du relecteur,
#6 présence, #11 présence manuelle, #7 dépôt, #12 clôture, #9 relecture, #10 tableau, #15 notes
reçues ; puis les Should #13, #14, #16 (utilisés par les écrans), Swagger (#37), seed réaliste
(#38). Frontend React : landing page shadcn/ui, choix du rôle avec gardes de route strictes (#39),
écrans formateur (session paramétrable, #40), étudiant et relecteur. Vérifié : `./mvnw verify`
92/92, `scripts/api-smoke.sh` 50/50 contre `docker compose up`, Vitest 24/24, Playwright Chromium
2/2 en mode normal et en mode visible (headed).

**Bloqué :** ~40 min sur Testcontainers : Docker 29 refuse l'API 1.32 négociée (fixée à 1.44 dans
le pom) puis une image `postgres:17-alpine` corrompue (`exec format error`, retéléchargée).
L'application ne démarrait pas du tout (colonne `status` au lieu de `statut`) : trouvé par le
premier test d'intégration. ~25 min de builds Docker lents (`dependency:go-offline`), résolus par
un cache Maven BuildKit. Les tests e2e ont révélé que le parcours supposait une base vide
(moyenne attendue 18, réelle 15 avec le seed) : c'est le test qui était faux, corrigé (#55).

**IA :** Claude Code a écrit le code et les tests sous ma direction, un sous-agent a construit le
frontend dans un worktree séparé. Vérifications : chaque règle RGx a un test rouge puis vert ;
j'ai rejeté le rabattement des erreurs sur les statuts imposés et imposé les vrais statuts HTTP
(DEC-9, #21 : 404 pour une ressource inconnue, 429 pour RG3) ; revue des bugs trouvés en test
(blocage RG3 annulé par le rollback, note 12.5 tronquée à 12, requête N+1 du tableau,
`relecturesEnAttente` mal défini, « ajouté par le formateur » invisible : #41). Écart assumé au
plan du CDC §10 : les Should #13/#14/#16 ont été faites avant le jalon v0.1 car les écrans
relecteur et étudiant en dépendent.

---

## Étape 3 — Enveloppe

**Fait :**

**Bloqué :**

**IA :**

**Ce que j'ai sorti du périmètre pour absorber le changement, et pourquoi :**

---

## Étape 4 — Version finale

**Fait :**

**Bloqué :**

**IA :**

---

## Étape 5 — Soumission

**Fait :**

**Ce que je referais autrement avec une journée de plus :**
