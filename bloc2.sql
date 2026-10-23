-- =====================================================
-- BLOC 2 : OFFRES D'EMPLOI ET CANDIDATURES
-- (offres issues des lignes validées par le DG)
-- =====================================================

-- 1. TABLES DE RÉFÉRENCE (offres et candidatures)

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

-- 4. CVTHÈQUE

CREATE TABLE candidat (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    mot_de_passe VARCHAR(100) NOT NULL,      -- hash BCrypt (60 caractères), jamais le mot de passe en clair
    telephone VARCHAR(30),
    chemin_cv VARCHAR(255),                  -- fichier PDF/Docx en pièce justificative
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. CANDIDATURE (lien candidat <-> offre)
-- Un candidat ne peut postuler qu'une fois à une même offre.
-- Supprimer un candidat supprime ses candidatures ; une offre ayant des
-- candidatures ne peut pas être supprimée (RESTRICT).

CREATE TABLE candidature (
    id SERIAL PRIMARY KEY,
    offre_emploi_id INT NOT NULL REFERENCES offre_emploi(id) ON DELETE RESTRICT,
    candidat_id INT NOT NULL REFERENCES candidat(id) ON DELETE CASCADE,
    date_candidature TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    annee_experience INT NOT NULL CHECK (annee_experience >= 0),
    dernier_diplome VARCHAR(150) NOT NULL,
    pretention_salariale DECIMAL(12, 2)
        CHECK (pretention_salariale IS NULL OR pretention_salariale >= 0),
    statut_candidature_id INT NOT NULL REFERENCES statut_candidature(id) ON DELETE RESTRICT,
    CONSTRAINT uq_candidature_offre_candidat UNIQUE (offre_emploi_id, candidat_id)
);

CREATE INDEX idx_candidature_candidat ON candidature(candidat_id);
CREATE INDEX idx_candidature_offre_statut ON candidature(offre_emploi_id, statut_candidature_id);

-- 6. RATTRAPAGE (facultatif) : créer les offres BROUILLON pour les lignes déjà
-- validées par le DG. Inutile dans bloc2.sql lors d'une réinitialisation (les
-- données de donnebloc1.sql arrivent après) : à lancer APRÈS donnebloc1.sql.
-- ⚠ Adapte les codes 'VALIDEE' et 'DG' à ceux de ta base.
--
-- INSERT INTO offre_emploi (ligne_demande_id, titre_offre, description_offre, statut_offre_id)
-- SELECT l.id, p.titre, p.description, (SELECT id FROM statut_offre WHERE code = 'BROUILLON')
-- FROM ligne_demande_recrutement l
-- JOIN poste p ON p.id = l.poste_id
-- JOIN statut_ligne sl ON sl.id = l.statut_ligne_id
-- WHERE sl.code = 'VALIDEE'
--   AND EXISTS (
--       SELECT 1 FROM historique_validation_ligne h
--       WHERE h.ligne_demande_id = l.id AND h.etape = 'DG'
--   )
--   AND NOT EXISTS (
--       SELECT 1 FROM offre_emploi o WHERE o.ligne_demande_id = l.id
--   );

-- 7. DONNÉES DE TEST (facultatif : décommente pour voir la liste sans formulaire de candidature)
--
-- INSERT INTO candidat (nom, prenom, email, telephone) VALUES
--     ('Rakoto', 'Jean', 'jean.rakoto@test.com', '034 00 000 00'),
--     ('Rabe', 'Aina', 'aina.rabe@test.com', '033 00 000 01');
--
-- INSERT INTO candidature (offre_emploi_id, candidat_id, annee_experience, dernier_diplome,
--                          pretention_salariale, statut_candidature_id)
-- SELECT o.id, c.id, 3, 'Licence', 1200000,
--        (SELECT id FROM statut_candidature WHERE code = 'RECUE')
-- FROM offre_emploi o
-- CROSS JOIN candidat c
-- WHERE o.date_publication IS NOT NULL;