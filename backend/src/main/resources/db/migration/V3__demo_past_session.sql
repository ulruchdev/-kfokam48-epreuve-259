-- ============================================================
-- V3__demo_past_session.sql  (issue #38)
-- A realistic closed past session so the reviewer opens a non-empty dashboard:
-- attendance by code and one added by the trainer (RG12), two reviewed exercises,
-- one review never rendered (Q11), one absent student. Students are looked up by
-- name (V2) so the seed never depends on generated ids. No schema change (D2 unchanged).
-- ============================================================

INSERT INTO session (promotion_id, titre, code, ouverture_at, expiration_at, fin_at, statut)
SELECT p.id, 'Séance 1 — Introduction à Spring Boot', 'INTRO1',
       now() - interval '7 days',
       now() - interval '7 days' + interval '15 minutes',
       now() - interval '7 days' + interval '2 hours',
       'CLOTUREE'
FROM promotion p WHERE p.nom = 'KFOKAM48 - Promotion 2026';

-- Attendance: four by code, one added by the trainer (RG12, Q14); Ngo Bassa Ines absent
INSERT INTO presence (session_id, etudiant_id, source, marque_a)
SELECT s.id, e.id, v.source, s.ouverture_at + interval '5 minutes'
FROM session s
JOIN (VALUES ('Abanda Meli', 'ETUDIANT'), ('Bello Sara', 'ETUDIANT'), ('Djoumessi Paul', 'ETUDIANT'),
             ('Eyenga Grace', 'ETUDIANT'), ('Kamga Yannick', 'FORMATEUR')) AS v(nom, source) ON true
JOIN etudiant e ON e.nom = v.nom
WHERE s.code = 'INTRO1' AND s.statut = 'CLOTUREE';

-- Exercises: two reviewed, one whose review was never rendered (Q11)
INSERT INTO exercice (session_id, etudiant_id, lien, statut, depose_a)
SELECT s.id, e.id, v.lien, v.statut, s.ouverture_at + interval '1 hour'
FROM session s
JOIN (VALUES ('Abanda Meli', 'https://github.com/abanda-meli/spring-intro', 'RELU'),
             ('Bello Sara', 'https://github.com/bello-sara/spring-intro', 'RELU'),
             ('Djoumessi Paul', 'https://github.com/djoumessi-paul/spring-intro', 'EN_ATTENTE_RELECTURE'))
     AS v(nom, lien, statut) ON true
JOIN etudiant e ON e.nom = v.nom
WHERE s.code = 'INTRO1' AND s.statut = 'CLOTUREE';

-- Reviews: never the author (RG4), one per exercise (RG5), integer grades (RG8)
INSERT INTO relecture (exercice_id, relecteur_id, note, commentaire, rendue, rendue_a)
SELECT x.id, r.id, v.note, v.commentaire, v.rendue,
       CASE WHEN v.rendue THEN s.ouverture_at + interval '90 minutes' END
FROM session s
JOIN exercice x ON x.session_id = s.id
JOIN etudiant a ON a.id = x.etudiant_id
JOIN (VALUES ('Abanda Meli', 'Bello Sara', 15, 'Couches bien séparées, tests lisibles.', true),
             ('Bello Sara', 'Djoumessi Paul', 12, 'Fonctionne, mais la validation des entrées manque.', true),
             ('Djoumessi Paul', 'Eyenga Grace', NULL, NULL, false))
     AS v(auteur, relecteur, note, commentaire, rendue) ON v.auteur = a.nom
JOIN etudiant r ON r.nom = v.relecteur
WHERE s.code = 'INTRO1' AND s.statut = 'CLOTUREE';
