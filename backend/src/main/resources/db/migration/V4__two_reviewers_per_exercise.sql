-- ============================================================
-- V4__two_reviewers_per_exercise.sql  (issue #62, step-3 change of need)
-- RG5 revised: each exercise is reviewed by two distinct peers, never more.
-- V1-V3 are never edited; existing data survive (DEC-14).
-- ============================================================

-- One review per exercise is no longer the rule
ALTER TABLE relecture DROP CONSTRAINT relecture_exercice_id_key;

-- Rank of the reviewer (1 or 2): existing reviews become rank 1
ALTER TABLE relecture ADD COLUMN rang SMALLINT NOT NULL DEFAULT 1;
ALTER TABLE relecture ALTER COLUMN rang DROP DEFAULT;
ALTER TABLE relecture ADD CONSTRAINT chk_relecture_rang CHECK (rang IN (1, 2));

-- The database itself refuses a third review and the same reviewer twice
ALTER TABLE relecture ADD CONSTRAINT uniq_relecture_exercice_rang UNIQUE (exercice_id, rang);
ALTER TABLE relecture ADD CONSTRAINT uniq_relecture_exercice_relecteur UNIQUE (exercice_id, relecteur_id);

-- Exercises that have a single reviewer now wait for their second one (RG15);
-- in a closed session their grade simply stays provisional (RG16)
UPDATE exercice x
SET statut = 'EN_ATTENTE_AFFECTATION'
WHERE statut IN ('EN_ATTENTE_RELECTURE', 'RELU')
  AND (SELECT count(*) FROM relecture r WHERE r.exercice_id = x.id) < 2;
