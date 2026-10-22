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