-- ============================================================
-- V1__schema.sql
-- Exact mirror of docs/diagrammes/D2-data-model.md
-- Any schema change must update D2 in the same PR (CDC section 8).
-- ============================================================

CREATE TABLE promotion (
    id   BIGSERIAL PRIMARY KEY,
    nom  VARCHAR(120) NOT NULL UNIQUE
);

CREATE TABLE etudiant (
    id              BIGSERIAL PRIMARY KEY,
    promotion_id    BIGINT NOT NULL REFERENCES promotion(id),
    nom             VARCHAR(120) NOT NULL,
    -- RG3 (Q4): failed code attempts + 2-minute lock window.
    -- Stored per student (client wording: "bloquez-le deux minutes").
    tentatives_code INT         NOT NULL DEFAULT 0,
    bloque_jusqu_a  TIMESTAMPTZ,
    UNIQUE (promotion_id, nom)
);

CREATE TABLE session (
    id            BIGSERIAL PRIMARY KEY,
    promotion_id  BIGINT NOT NULL REFERENCES promotion(id),
    titre         VARCHAR(120) NOT NULL,
    code          VARCHAR(10)  NOT NULL,
    ouverture_at  TIMESTAMPTZ NOT NULL,
    expiration_at TIMESTAMPTZ NOT NULL,            -- RG1 (Q2): ouverture_at + 15 min
    fin_at        TIMESTAMPTZ NOT NULL,            -- DEC-2: ouverture_at + dureeMinutes (default 120)
    statut        VARCHAR(12) NOT NULL DEFAULT 'OUVERTE'
                  CHECK (statut IN ('OUVERTE', 'TERMINEE', 'CLOTUREE'))
);

-- DEC-5: the presence code is unique among non-closed sessions
CREATE UNIQUE INDEX uniq_session_code_active
    ON session (code) WHERE statut <> 'CLOTUREE';

CREATE INDEX idx_session_promotion ON session (promotion_id);

CREATE TABLE presence (
    id          BIGSERIAL PRIMARY KEY,
    session_id  BIGINT NOT NULL REFERENCES session(id),
    etudiant_id BIGINT NOT NULL REFERENCES etudiant(id),
    source      VARCHAR(10) NOT NULL CHECK (source IN ('ETUDIANT', 'FORMATEUR')),  -- RG12 (Q14)
    marque_a    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (session_id, etudiant_id)                             -- Q2/Q3: one presence per session
);

CREATE INDEX idx_presence_etudiant ON presence (etudiant_id);

CREATE TABLE exercice (
    id          BIGSERIAL PRIMARY KEY,
    session_id  BIGINT NOT NULL REFERENCES session(id),
    etudiant_id BIGINT NOT NULL REFERENCES etudiant(id),
    lien        TEXT NOT NULL,
    statut      VARCHAR(25) NOT NULL DEFAULT 'EN_ATTENTE_AFFECTATION'
                CHECK (statut IN ('EN_ATTENTE_AFFECTATION', 'EN_ATTENTE_RELECTURE', 'RELU')),
    depose_a    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (session_id, etudiant_id)                             -- 409 EXERCICE_DEJA_DEPOSE
);

CREATE INDEX idx_exercice_session ON exercice (session_id);

CREATE TABLE relecture (
    id           BIGSERIAL PRIMARY KEY,
    exercice_id  BIGINT NOT NULL UNIQUE REFERENCES exercice(id), -- RG5 (Q6): exactly one reviewer
    relecteur_id BIGINT NOT NULL REFERENCES etudiant(id),        -- RG4 (Q5): must differ from author (service)
    note         INT CHECK (note BETWEEN 0 AND 20),              -- RG8 (Q9), null until rendered
    commentaire  TEXT,
    rendue       BOOLEAN NOT NULL DEFAULT FALSE,
    rendue_a     TIMESTAMPTZ,
    maj_a        TIMESTAMPTZ                                     -- RG9 (Q10/DEC-1): amendment timestamp
);

CREATE INDEX idx_relecture_relecteur ON relecture (relecteur_id);
