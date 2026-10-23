CREATE TABLE candidat (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    telephone VARCHAR(30),
    chemin_cv VARCHAR(255),                  -- fichier PDF/Docx en pièce justificative
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. OFFRE D'EMPLOI
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
    intitule VARCHAR(150) NOT NULL,
    type_critere VARCHAR(30) NOT NULL
        CHECK (type_critere IN ('EXPERIENCE', 'DIPLOME', 'COMPETENCE')),
    valeur_requise VARCHAR(100) NOT NULL,
    est_obligatoire BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_offre_statut ON offre_emploi(statut_offre_id);
CREATE INDEX idx_critere_offre ON critere_offre(offre_emploi_id);


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
 
-- 3. DONNÉES DE TEST (facultatif : décommente pour voir la liste sans formulaire de candidature)
INSERT INTO candidat (nom, prenom, email, telephone) VALUES
    ('Rakoto', 'Jean', 'jean.rakoto@test.com', '034 00 000 00'),
    ('Rabe', 'Aina', 'aina.rabe@test.com', '033 00 000 01');

INSERT INTO candidature (offre_emploi_id, candidat_id, annee_experience, dernier_diplome,
                         pretention_salariale, statut_candidature_id)
SELECT o.id, c.id, 3, 'Licence', 1200000,
       (SELECT id FROM statut_candidature WHERE code = 'RECUE')
FROM offre_emploi o
CROSS JOIN candidat c
WHERE o.date_publication IS NOT NULL;
