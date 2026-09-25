# Backlog — Issues du projet

> Source de vérité : les issues GitHub (https://github.com/ulruchdev/kfokam48-epreuve-259/issues).
> Ce fichier est un instantané de lecture, mis à jour à chaque évolution du backlog.
> Priorités : **Must** = périmètre `[JALON] v0.1` · **Should** = candidat pour v1.0 · **Could** = polish.

## Vue d'ensemble

| # | Titre | Priorité | EF | RG couvertes | Statut |
|---|---|---|---|---|---|
| [#17](https://github.com/ulruchdev/kfokam48-epreuve-259/issues/17) | Démarrer l'app avec `docker compose up` + données de démo | Must | — | B1, B5, B6, F1, ENF5 | OPEN |
| [#5](https://github.com/ulruchdev/kfokam48-epreuve-259/issues/5) | Ouvrir une session, obtenir un code 15 min | Must | EF1 | RG1, DEC-2, DEC-5 | OPEN |
| [#6](https://github.com/ulruchdev/kfokam48-epreuve-259/issues/6) | Marquer sa présence avec le code | Must | EF2 | RG1, RG2, RG3, RG15 | OPEN |
| [#7](https://github.com/ulruchdev/kfokam48-epreuve-259/issues/7) | Déposer le lien de son exercice | Must | EF3 | RG10, RG14, DEC-4 | OPEN |
| [#8](https://github.com/ulruchdev/kfokam48-epreuve-259/issues/8) | Affecter un relecteur au hasard parmi les présents | Must | EF4 | RG4, RG5, RG6, RG15, DEC-3, DEC-7 | OPEN |
| [#9](https://github.com/ulruchdev/kfokam48-epreuve-259/issues/9) | Rendre sa relecture (note 0–20 + commentaire) | Must | EF5 | RG4, RG7, RG8 | OPEN |
| [#10](https://github.com/ulruchdev/kfokam48-epreuve-259/issues/10) | Voir le tableau récapitulatif de la promotion | Must | EF6 | RG9, RG13, RG15, DEC-8, ENF2 | OPEN |
| [#11](https://github.com/ulruchdev/kfokam48-epreuve-259/issues/11) | Ajouter une présence manuelle « par le formateur » | Must | EF7 | RG12 | OPEN |
| [#12](https://github.com/ulruchdev/kfokam48-epreuve-259/issues/12) | Clôturer la session | Must | EF8 | RG9, RG10 | OPEN |
| [#15](https://github.com/ulruchdev/kfokam48-epreuve-259/issues/15) | Voir ses notes et commentaires (relecteur anonyme) | Must | EF9 | RG7 | OPEN |
| [#4](https://github.com/ulruchdev/kfokam48-epreuve-259/issues/4) | Choisir son identité dans la liste | Must | EF10 | Q1, DEC-6 | OPEN |
| [#13](https://github.com/ulruchdev/kfokam48-epreuve-259/issues/13) | Corriger sa relecture jusqu'à clôture | Should | EF11 | RG8, RG9, DEC-1, DEC-8 | OPEN |
| [#14](https://github.com/ulruchdev/kfokam48-epreuve-259/issues/14) | Remplacer le lien avant début de relecture | Should | EF12 | RG11 | OPEN |
| [#16](https://github.com/ulruchdev/kfokam48-epreuve-259/issues/16) | Lister ses relectures assignées | Should | EF13 | — | OPEN |

## Ordre d'exécution prévu (v0.1 — étape 2)

1. **#17** — squelette + Docker + Flyway + démo (fondations, tout le reste en dépend)
2. **#5** — sessions (le code de présence ouvre le flux)
3. **#6** — présences (dépend de #5)
4. **#7** — dépôt d'exercice (dépend de #5, #6)
5. **#8** — affectation relecteur (dépend de #6, #7)
6. **#9** — relecture (dépend de #8)
7. **#10** — tableau (agrège tout)
8. **#11**, **#12** — présence manuelle, clôture (transverses)
9. **#4**, **#15** — écrans frontend identité et notes

Les Should (#13, #14, #16) ne sont attaqués qu'après `[JALON] v0.1`, si l'enveloppe
(étape 3) a été absorbée et que le temps le permet.
