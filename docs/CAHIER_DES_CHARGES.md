# Cahier des charges — Attendance & Peer Review Tracker (KFOKAM48)

**Auteur :** TASSE TUEKAM Ulruch Baudrel · matricule 259
**Version :** 1 · **Date :** 2026-09-25
**Frontend choisi :** **React (Vite + TypeScript)** — largest ecosystem, fastest iteration under hackathon time constraints, and the stack the candidate masters best; build verified with `npm run build`.

> Sources cited as `Qx` refer to `EPREUVE/CLIENT.md` (the 16 pre-answered client questions).
> Decisions taken **in place of the client** are cited as `DEC-x` (section 7).
> Rules `RGx` and requirements `EFx` are numbered so they can be referenced in issues,
> commit messages and test names.

---

## 1. Contexte et objectif

La direction de la formation KFOKAM48 gère aujourd'hui les présences et les exercices
de ses étudiants de façon manuelle : appel oral, exercices envoyés par messagerie,
notes compilées à la main dans un tableur. Ce processus est lent, sujet aux erreurs
et opaque : l'étudiant ne sait pas où il en est, le formateur reconstruit le suivi
à la fin du cycle.

L'application répond à ce problème par un flux simple en trois temps :

1. le **formateur** ouvre une session de cours et obtient un **code de présence** à
   durée limitée qu'il projette en salle ;
2. l'**étudiant** marque sa présence avec ce code, dépose le **lien** de son exercice,
   et est désigné par le système comme **relecteur** de l'exercice d'un pair qu'il note
   de 0 à 20 avec un commentaire ;
3. le **formateur** suit le tout dans un **tableau** : présences, exercices déposés,
   moyenne des notes reçues, relectures en attente.

L'objectif produit n'est pas l'exhaustivité mais la **fiabilité du suivi** : chaque
règle annoncée par le client est appliquée côté serveur, et le tableau est la source
de vérité unique.

## 2. Acteurs et rôles

| Acteur | Ce qu'il peut faire | Ce qu'il ne peut pas faire |
|---|---|---|
| **Formateur** | Ouvrir une session (EF1), voir le tableau (EF6), ajouter une présence manuelle (EF7), clôturer la session (EF8), ajuster l'heure de fin (DEC-2) | Marquer une présence pour lui-même, noter un exercice, modifier une note |
| **Étudiant** | Choisir son identité dans la liste de sa promotion (EF10), marquer sa présence (EF2), déposer puis remplacer le lien de son exercice (EF3, EF12), consulter ses notes et commentaires reçus (EF9) | S'auto-relire (RG4), voir le nom de son relecteur (RG7), déposer sans présence (RG14), déposer après clôture (RG10) |
| **Relecteur** *(= étudiant désigné, pas un acteur distinct)* | Rendre sa relecture note + commentaire (EF5), la corriger jusqu'à clôture (EF11), lister ses relectures assignées (EF13) | Relire son propre exercice (RG4), corriger après clôture (RG9), être nommé à l'étudiant relu (RG7) |

**Décision de modélisation :** le relecteur **n'est pas** un acteur distinct ni une
entité séparée — c'est un **étudiant dans un état transitoire** (assignation stockée
dans `RELECTURE.relecteur_id` → `ETUDIANT.id`). Conséquences : aucune table
`RELECTEUR`, les règles RG4 (pas d'auto-relecture) et RG6 (tirage parmi les présents)
sont des contraintes de service sur la relation étudiant↔exercice, et un étudiant
peut être à la fois auteur d'un exercice et relecteur d'un autre dans la même session.

**Automatismes système** (pas un acteur, mais des comportements à part entière) :
génération du code, expiration (RG1), compteur de tentatives et blocage (RG3),
tirage au sort du relecteur (RG6/RG15), calcul de la moyenne (EF6, F3).

## 3. Périmètre

**Inclus dans cette version :**

- Gestion des sessions de cours : ouverture avec code de présence expirant (15 min),
  heure de fin, clôture manuelle du formateur
- Présence par code avec anti-énumération (5 erreurs → blocage 2 min) et présence
  manuelle formateur tracée (`source = FORMATEUR`)
- Dépôt de l'exercice sous forme de **lien URI**, remplaçable avant début de relecture
- Affectation automatique d'**un** relecteur par exercice, au hasard parmi les
  présents de la session, auteur exclu, avec réaffectation tant que l'affectation
  n'a pas pu se faire (étudiant seul présent)
- Relecture : note entière 0–20 + commentaire, correction possible jusqu'à clôture
- Tableau formateur : présences, exercices déposés, moyenne des notes reçues
  (null si aucune), relectures en attente
- Choix d'identité par liste (promotions → étudiants), **sans authentification** (Q1)
- Données de démonstration chargées au démarrage (1 promotion, ~6 étudiants)
- Démarrage complet par `docker compose up`

**Explicitement exclu :**

- Authentification, comptes, mots de passe, rôles applicatifs (Q1 : « Ne perdez pas
  de temps là-dessus ») — la confiance repose sur le choix d'identité en liste
- Téléversement de fichiers : **liens uniquement** (le client n'a jamais parlé de pièces jointes)
- Notifications (email, push, SMS) et temps réel (WebSocket/SSE) : le tableau se rafraîchit au chargement
- Application mobile native : web responsive (ENF1)
- Interface d'administration des promotions/étudiants : les promotions, étudiants et
  sessions historiques sont chargés par migration de données de démonstration
- Import d'historique, exports (PDF/Excel), multi-tenant, i18n de l'interface
- Modération des commentaires de relecture

## 4. Exigences fonctionnelles

| Réf | Exigence | Critère d'acceptation | Priorité |
|---|---|---|---|
| EF1 | Le formateur ouvre une session de cours et obtient un code de présence | Quand je soumets `{titre, promotionId}` valide, alors je reçois `201` avec `{id, code, ouvertureAt, expirationAt}` et le code est unique parmi les sessions ouvertes | Must |
| EF2 | L'étudiant marque sa présence avec le code | Quand je saisis un code valide non expiré d'une session en cours, alors ma présence est enregistrée avec `source = ETUDIANT` et apparaît dans le tableau du formateur | Must |
| EF3 | L'étudiant dépose le lien de son exercice pour une session | Quand je soumets un lien URI valide pour une session où je suis présent (RG14), alors je reçois `201 {id, statut}` et mon compteur « exercices déposés » augmente dans le tableau | Must |
| EF4 | Le système affecte un relecteur à chaque exercice déposé | Quand un exercice est déposé et qu'au moins un autre étudiant est présent, alors le système choisit le relecteur au hasard parmi les présents, auteur exclu, et un seul relecteur est assigné (RG5, RG6) | Must |
| EF5 | Le relecteur rend sa relecture (note + commentaire) | Quand l'assigné soumet `{note, commentaire}` avec note entière 0–20, alors la relecture est enregistrée, l'exercice passe à `RELU`, et l'auteur voit la note et le commentaire sans le nom du relecteur | Must |
| EF6 | Le formateur voit le tableau récapitulatif de sa promotion | Quand j'ouvre le tableau d'une promotion connue, alors chaque étudiant apparaît avec ses présences, ses exercices déposés, sa moyenne (null si aucune note) et ses relectures en attente ; promotion inconnue → `404` | Must |
| EF7 | Le formateur ajoute une présence à la main | Quand j'ajoute une présence pour un étudiant sans code, alors la présence est créée avec `source = FORMATEUR` et « ajouté par le formateur » est visible dans le tableau | Must |
| EF8 | Le formateur clôture la session | Quand je clôture, alors plus aucun dépôt (RG10), plus de correction de relecture (RG9), et les relectures non rendues restent comptées « en attente » dans le tableau (Q11) | Must |
| EF9 | L'étudiant voit ses notes et commentaires reçus | Quand je consulte mes exercices, alors je vois pour chacun la note et le commentaire de la relecture rendue, sans jamais voir le nom du relecteur (RG7) | Must |
| EF10 | L'étudiant choisit son identité dans une liste | Quand j'ouvre l'application, je choisis ma promotion puis mon nom dans la liste des étudiants de cette promotion, sans mot de passe | Must |
| EF11 | Le relecteur corrige sa relecture jusqu'à la clôture | Quand je corrige ma relecture d'une session non clôturée (DEC-1), alors note et commentaire sont remplacés ; après clôture → `409 SESSION_CLOTUREE` | Should |
| EF12 | L'étudiant remplace le lien de son exercice tant que personne n'a relu | Quand je remplace mon lien avant toute relecture commencée, alors le lien est mis à jour ; si une relecture a commencé → `409 RELECTURE_COMMENCEE` | Should |
| EF13 | Le relecteur voit la liste de ses relectures assignées | Quand je consulte mes assignations, je vois les exercices à relire (lien, auteur) et celles déjà rendues | Should |

## 5. Exigences non fonctionnelles

| Réf | Exigence | Comment on la vérifie |
|---|---|---|
| ENF1 | L'interface de marquage de présence est utilisable sur un téléphone (usage réel en salle) | Les 3 écrans critiques (présence, dépôt, relecture) passent une largeur de 360 px sans scroll horizontal ; vérifié manuellement en responsive Chrome |
| ENF2 | Le tableau répond en moins de 2 s pour une promotion de 60 étudiants | La requête du tableau est une seule requête SQL agrégée (pas de N+1) ; mesuré avec 60 étudiants + 10 sessions de démo |
| ENF3 | Le code de présence n'est pas énumérable | Le code est généré aléatoirement (≥ 6 caractères alphanumériques), et 5 erreurs successives déclenchent un blocage de 2 min (RG3) ; test unitaire |
| ENF4 | Toute erreur 4xx/5xx renvoie le format imposé `{"code", "message"}` | `@RestControllerAdvice` unique ; test d'intégration sur chaque famille d'erreur du contrat — aucune stack trace possible |
| ENF5 | L'application démarre chez un tiers sans base locale | `docker compose up` seul ; les tests d'intégration utilisent Testcontainers ; vérifié par clonage dans un dossier vide |
| ENF6 | L'historique du code est versionné et lisible | Migrations Flyway commitées, `ddl-auto = validate` ; schéma = miroir du diagramme D2 |

## 6. Règles de gestion

| Réf | Règle | Source |
|---|---|---|
| RG1 | Le code de présence expire 15 minutes après l'ouverture de la session | Q2 |
| RG2 | Après la fin de la session, le code ne fonctionne plus (`410 CODE_EXPIRE`) | Q3 + DEC-2 |
| RG3 | Au bout de 5 erreurs de code, l'étudiant est bloqué 2 minutes (`400 TOO_MANY_ATTEMPTS` — code distinct dans le statut 400 déjà imposé, aucun statut ajouté sur une opération imposée, B2) | Q4 |
| RG4 | Un étudiant ne peut jamais relire son propre exercice (`403 AUTO_RELECTURE`) | Q5 |
| RG5 | Un seul relecteur par exercice (`UNIQUE(exercice_id)`) | Q6 |
| RG6 | Le relecteur est choisi par le système, au hasard, parmi les étudiants présents à la session, auteur exclu | Q7 |
| RG7 | L'étudiant relu voit la note et le commentaire, jamais le nom du relecteur | Q8 |
| RG8 | La note est un entier entre 0 et 20 (`400 NOTE_INVALIDE`) | Q9 |
| RG9 | La relecture est corrigeable par son assigné tant que la session n'est pas clôturée ; le `POST` sur une relecture déjà rendue reste `409 RELECTURE_DEJA_RENDUE` (le contrat impose le `409`) | Q10 retenu contre Q15 (DEC-1) |
| RG10 | Le dépôt d'exercice reste possible après la fin de session, jusqu'à la clôture par le formateur | Q12 |
| RG11 | Le lien de l'exercice est remplaçable tant qu'aucune relecture n'a commencé | Q13 |
| RG12 | Une présence ajoutée à la main par le formateur porte `source = FORMATEUR` | Q14 |
| RG13 | Une relecture jamais rendue laisse l'exercice « en attente », visible comme tel dans le tableau | Q11 |
| RG14 | Déposer un exercice exige d'avoir marqué sa présence à la session (`400 PRESENCE_REQUISE`) | DEC-4 (décision du candidat, sans source client) |
| RG15 | Si aucun relecteur éligible n'existe (étudiant seul présent), l'exercice reste `EN_ATTENTE_AFFECTATION` et le tirage est retenté à chaque nouvelle présence, jusqu'à la clôture | DEC-3 (trou du sujet) |

## 7. Zones d'ombre, hypothèses et contradictions

### Contradiction tranchée

| Réponses en conflit | Ce que j'ai choisi | Pourquoi |
|---|---|---|
| **Q10** (« le relecteur peut corriger sa note tant que la session n'est pas clôturée ») **vs Q15** (« une fois validée, c'est fini ») | **Q10 (DEC-1)** : correction possible via `PUT /api/relectures/{id}` jusqu'à clôture | Q10 est un usage **opérationnel** cohérent avec Q11 et Q12 : le client décrit lui-même une session qui « vit » jusqu'à sa clôture (dépôts tardifs, relectures en attente visibles). Q15 est une intention générale (« plus honnête pour tout le monde ») qui ne décrit aucun scénario. Le contrat imposé a lui-même besoin de Q10 pour rester cohérent : son `409 RELECTURE_DEJA_RENDUE` ne concerne que le `POST` (la **première** soumission), pas les corrections. Le `POST` imposé est respecté à la lettre ; la correction passe par un endpoint `PUT` **libre**. Q15 garde une portée réelle : après clôture, plus rien n'est modifiable (EF8) |

### Trous identifiés (personne ne les a vus dans CLIENT.md)

| Point | Hypothèse / décision | Conséquence |
|---|---|---|
| **La « fin de session » n'est définie nulle part**, alors que Q3 et Q12 la distinguent de la clôture | **DEC-2** : `dureeMinutes` optionnelle à l'ouverture (défaut 120 min, extension additive du corps imposé — les champs requis `{titre, promotionId}` restent inchangés), heure de fin `finAt` renvoyée en plus, ajustable par le formateur via `PUT /api/sessions/{id}` | Chronologie exploitable : `ouverture` → `+15 min : expiration du code` → `finAt : fin de session` → `clôture manuelle`. RG1/RG2 s'appliquent sur ces bornes |
| **Étudiant seul présent** : aucun relecteur éligible au dépôt | **DEC-3 (RG15)** : exercice `EN_ATTENTE_AFFECTATION`, tirage retenté à chaque nouvelle présence, jusqu'à clôture ; visible dans le tableau (Q11) | Évite un `409` qui contredirait Q12 (« dépôt toujours possible jusqu'à clôture ») |
| **Aucune opération pour lister promotions/étudiants** alors que Q1 impose le choix d'identité en liste et que le contrat impose `promotionId`/`etudiantId` | Ajout des endpoints **libres** `GET /api/promotions` et `GET /api/promotions/{id}/etudiants` au contrat complété | Sans eux, l'écran EF10 est impossible : c'est le trou qui bloque tout le flux |

### Hypothèses sans réponse client (décisions du candidat, assumées)

| Point | Réponse client (Qx) ou hypothèse | Décision retenue | Conséquence |
|---|---|---|---|
| Déposer exige-t-il d'être présent ? | Aucune Qx ne le dit ; Q16 donne deux compteurs **indépendants** | **DEC-4 (RG14)** : la présence **est exigée** — un exercice est la preuve de participation à la session | Nouveau code `400 PRESENCE_REQUISE` (extension additive, statut déjà imposé) ; un étudiant absent d'une session ne peut pas y déposer |
| Une promotion peut-elle avoir plusieurs sessions actives ? | Silence du client | **DEC-5** : oui, mais le **code** est unique parmi les sessions non clôturées (sinon le tirage du relecteur par session serait ambigu) | `UNIQUE` partiel côté code : unicité vérifiée en service parmi `statut != CLOTUREE` |
| Que voit un étudiant des sessions disponibles ? | Silence | **DEC-6** : uniquement les sessions de sa promotion, les plus récentes d'abord (`GET /api/sessions?promotionId=`) | L'écran étudiant liste les sessions où agir (présence / dépôt / relecture) |
| Le relecteur peut-il être assigné deux fois dans la même session ? | Silence | **DEC-7** : le tirage équilibre — un présent déjà relecteur d'un exercice en attente de la session n'est retiré du hasard que si tous les autres présents éligibles le sont aussi | Évite qu'un étudiant concentre toutes les relectures quand 3+ présents |
| La moyenne du tableau inclut-elle les notes corrigées ? | Silence | **DEC-8** : oui — la moyenne est recalculée sur les notes **courantes** (RG9), calculée par l'API, jamais par le front (F3) | `moyenne` = moyenne arithmétique des notes rendues de la promotion, `null` si aucune |

## 8. Contraintes techniques

**Imposées par le sujet :**

- **B1** Java 17+ (Java 21 retenu), Maven, wrapper `mvnw` commité
- **B2** Contrat `api/contrat.yaml` respecté à la lettre : chemins, verbes, codes de statut, format d'erreur `{"code", "message"}` — les 5 opérations imposées sont exactes ; extensions **additives uniquement** (documentées dans le contrat)
- **B3** Séparation contrôleur / service / repository ; aucune requête base dans un contrôleur ; aucune entité JPA exposée en JSON — DTO en frontière
- **B4** Validation des entrées (`@Valid`) et gestion centralisée des erreurs (`@RestControllerAdvice`) — aucune stack trace renvoyée
- **B5** Schéma versionné par **Flyway**, migrations commitées ; `ddl-auto = validate`
- **B6** Tests : au moins 1 test unitaire sur une règle métier réelle + 1 test d'intégration sur un endpoint, tournant sur poste vierge (**Testcontainers PostgreSQL**)
- **F1** React (Vite + TypeScript) déclaré et justifié en tête de README, build vérifié
- **F2** Trois écrans : formateur (session + tableau), étudiant (présence + dépôt), relecteur (relecture)
- **F3** Appels API dans une couche dédiée, états de chargement/erreur gérés, **aucune règle métier dupliquée** : la moyenne vient de l'API
- Démarrage : `docker compose up` (db + backend + frontend) avec **données de démonstration** au démarrage

**Choisies par le candidat :**

- PostgreSQL 17 (contrainte de la stack de formation), Flyway (B5), pas de cache
- Tests : JUnit 5 + AssertJ + Mockito (unitaire), Testcontainers (intégration), Vitest + Testing Library (front)
- Git : voir `CONTRIBUTING.md` — une branche par issue, une PR par branche, commits conventionnels atomiques, 3 jalons `[JALON]` poussés dans l'ordre
- Messages d'erreur API en **français** (imposé par le contrat) ; le reste du livrable en anglais

## 9. Livrables

- `docs/CAHIER_DES_CHARGES.md` — ce document
- `docs/diagrammes/` — D1 cas d'utilisation, D2 modèle de données (miroir des migrations), D3 séquence « marquer sa présence » (codes HTTP alignés au contrat), D4 bonus états-transitions de l'exercice
- `docs/JOURNAL.md` — une entrée par étape
- `api/contrat.yaml` — contrat complété (5 opérations imposées + extensions additives)
- `backend/` — Spring Boot (Java 21, Maven, `mvnw`, Flyway, Testcontainers)
- `frontend/` — React (Vite, TypeScript), 3 écrans
- `docker-compose.yml` + Dockerfiles — démarrage complet avec données de démo
- Backlog : issues GitHub avec critères d'acceptation, priorités Must/Should/Could, renvois `EFx`/`RGx`
- `CHANGELOG.md` à l'étape 4, `SOUMISSION.md` à l'étape 6

## 10. Démarche prévue

1. **Étape 1 (celle-ci)** : contrat figé → issues du backlog créées → commit `[JALON] analyse` poussé **avant tout code**.
2. **Étape 2 (v0.1)** : stories **Must** uniquement (EF1→EF10), une branche/issue, une PR/branche, Docker + migrations d'abord, TDD sur chaque règle RGx. Jalon `[JALON] v0.1` poussé.
3. **Étape 3 (enveloppe)** : issue ouverte **avant** de coder, bug reproduit par test, migration versionnée, contrat et analyse mis à jour dans des commits qui le disent ; périmètre sacrifié explicité dans le JOURNAL.
4. **Étape 4 (v1.0)** : stories Should restantes si le temps le permet, `CHANGELOG.md`, README testé depuis un clone vierge, jalon `[JALON] v1.0`.
5. **Étape 5** : épreuve `git-lab` sur un dépôt séparé (`kfokam48-gitlab-259`), jamais mélangée au projet.
6. **Étape 6** : `SOUMISSION.md` téléversé avec les deux hash complets, liens vérifiés en navigation privée — **avant 18h00**.

**Plan de repli si retard :** les stories **Should** (EF11–EF13) sont sacrifiées d'abord
(avec le D4 bonus), jamais la mise à jour de l'analyse après l'enveloppe (3 pts) ni
l'hygiène Git (15 pts) ni la soumission.

**Definition of Done — un ticket est terminé quand :**

- ses critères d'acceptation sont vérifiables par un test ou une démonstration précise
- l'entrée JOURNAL de l'étape 1 est complétée par la relecture qualité ci-dessus
- les tests (unitaire et/ou intégration) passent : `./mvnw verify` vert
- `docker compose up` reste vert après merge
- la règle `RGx` concernée est citée dans le message de commit et, si pertinente, dans le nom du test
- la PR est mergée dans `main` par le workflow de `CONTRIBUTING.md`, l'issue fermée
- l'entité exposée passe par un DTO, les erreurs passent par le handler central

---

## Journal des révisions

| Version | Quand | Ce qui a changé et pourquoi |
|---|---|---|
| 1 | 2026-09-25 | Version initiale (étape 1) — sera révisée après l'ouverture de l'enveloppe (étape 3) |
