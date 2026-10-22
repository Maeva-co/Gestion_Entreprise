-- =====================================================
-- BLOC 2 : OFFRES D'EMPLOI (issues des lignes validées par le DG)
-- =====================================================

-- 1. TABLES DE RÉFÉRENCE

-- Tables de référence : offres et candidatures

CREATE TABLE statut_offre (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    libelle VARCHAR(100) NOT NULL
);

CREATE TABLE statut_candidature (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    libelle VARCHAR(100) NOT NULL,
    ordre INT NOT NULL -- position dans le pipeline (utile pour un affichage kanban)
);

INSERT INTO statut_offre (code, libelle) VALUES
    ('BROUILLON', 'Brouillon'),
    ('PUBLIEE', 'Publiée'),
    ('SUSPENDUE', 'Suspendue'),
    ('CLOTUREE', 'Clôturée');

INSERT INTO statut_candidature (code, libelle, ordre) VALUES
    ('RECUE', 'Reçue', 1),
    ('PRESELECTIONNE', 'Présélectionnée', 2),
    ('TEST_EN_COURS', 'Test en cours', 3),
    ('ENTRETIEN', 'Entretien', 4),
    ('OFFRE_EMISE', 'Offre émise', 5),
    ('EMBAUCHE', 'Embauchée', 6),
    ('REFUSE', 'Refusée', 7);

INSERT INTO statut_offre (code, libelle) VALUES
    ('BROUILLON', 'Brouillon'),
    ('PUBLIEE', 'Publiée'),
    ('SUSPENDUE', 'Suspendue'),
    ('CLOTUREE', 'Clôturée');

INSERT INTO statut_candidature (code, libelle, ordre) VALUES
    ('RECUE', 'Reçue', 1),
    ('PRESELECTIONNE', 'Présélectionnée', 2),
    ('TEST_EN_COURS', 'Test en cours', 3),
    ('ENTRETIEN', 'Entretien', 4),
    ('OFFRE_EMISE', 'Offre émise', 5),
    ('EMBAUCHE', 'Embauchée', 6),
    ('REFUSE', 'Refusée', 7);


-- 2. OFFRE D'EMPLOI
-- Une ligne de demande validée par le DG = une offre (UNIQUE sur ligne_demande_id).
-- Le poste, le nombre de postes, le salaire proposé... se lisent via la ligne.

CREATE TABLE offre_emploi (
    id SERIAL PRIMARY KEY,
    ligne_demande_id INT NOT NULL UNIQUE
        REFERENCES ligne_demande_recrutement(id) ON DELETE RESTRICT,
    titre_offre VARCHAR(200) NOT NULL,
    description_offre TEXT,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_publication TIMESTAMP,
    date_cloture TIMESTAMP,
    statut_offre_id INT NOT NULL REFERENCES statut_offre(id) ON DELETE RESTRICT,
    CONSTRAINT chk_offre_dates
        CHECK (date_publication IS NULL OR date_cloture IS NULL OR date_cloture >= date_publication)
);

-- 3. CRITÈRES DE FILTRAGE D'UNE OFFRE

CREATE TABLE critere_offre (
    id SERIAL PRIMARY KEY,
    offre_emploi_id INT NOT NULL REFERENCES offre_emploi(id) ON DELETE CASCADE,
    intitule VARCHAR(150) NOT NULL,           -- ex. Années d'expérience Java
    type_critere VARCHAR(30) NOT NULL
        CHECK (type_critere IN ('EXPERIENCE', 'DIPLOME', 'COMPETENCE')),
    valeur_requise VARCHAR(100) NOT NULL,     -- ex. 3, Bac+5
    est_obligatoire BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_offre_statut ON offre_emploi(statut_offre_id);
CREATE INDEX idx_critere_offre ON critere_offre(offre_emploi_id);

-- 4. RATTRAPAGE : créer les offres (en BROUILLON) pour les lignes déjà validées par le DG
-- ⚠ Adapte les codes 'VALIDEE' et 'DG' à ceux de ta base avant d'exécuter.

INSERT INTO offre_emploi (ligne_demande_id, titre_offre, description_offre, statut_offre_id)
SELECT l.id, p.titre, p.description, (SELECT id FROM statut_offre WHERE code = 'BROUILLON')
FROM ligne_demande_recrutement l
JOIN poste p ON p.id = l.poste_id
JOIN statut_ligne sl ON sl.id = l.statut_ligne_id
WHERE sl.code = 'VALIDEE'
  AND EXISTS (
      SELECT 1 FROM historique_validation_ligne h
      WHERE h.ligne_demande_id = l.id AND h.etape = 'DG'
  )
  AND NOT EXISTS (
      SELECT 1 FROM offre_emploi o WHERE o.ligne_demande_id = l.id
  );