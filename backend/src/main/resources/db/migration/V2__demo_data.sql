-- ============================================================
-- V2__demo_data.sql
-- Demo data required by the assessment: the reviewer must open a
-- non-empty application (issue #17).
-- ============================================================

INSERT INTO promotion (nom) VALUES ('KFOKAM48 - Promotion 2026');

INSERT INTO etudiant (promotion_id, nom) VALUES
    (1, 'Abanda Meli'),
    (1, 'Bello Sara'),
    (1, 'Djoumessi Paul'),
    (1, 'Eyenga Grace'),
    (1, 'Kamga Yannick'),
    (1, 'Ngo Bassa Ines');
