-- ==========================================
-- BLOC 1 : EXPRESSION DU BESOIN EN PERSONNEL
-- ==========================================


-- 1. TABLES DE RÉFÉRENCE (STATUTS & PARAMÈTRES)

CREATE TABLE role (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    libelle VARCHAR(100) NOT NULL
);

CREATE TABLE departement (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    description TEXT
);

CREATE TABLE grille_salariale (
    id SERIAL PRIMARY KEY,
    niveau VARCHAR(50) NOT NULL,
    salaire_embauche NUMERIC(12, 2) NOT NULL,
    salaire_anciennete NUMERIC(12, 2) NOT NULL,
    salaire_revalorise NUMERIC(12, 2) NOT NULL
);

CREATE TABLE statut_poste (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    libelle VARCHAR(100) NOT NULL
);

CREATE TABLE statut_demande (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    libelle VARCHAR(100) NOT NULL
);

CREATE TABLE statut_ligne (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    libelle VARCHAR(100) NOT NULL
);

CREATE TABLE statut_validation (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    libelle VARCHAR(100) NOT NULL
);

-- 2. TABLES MÉTIERS PRINCIPALES

CREATE TABLE utilisateur (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    mot_de_passe VARCHAR(255) NOT NULL,
    departement_id INT REFERENCES departement(id) ON DELETE SET NULL,
    role_id INT REFERENCES role(id) ON DELETE RESTRICT
);

CREATE TABLE poste (
    id SERIAL PRIMARY KEY,
    titre VARCHAR(150) NOT NULL,
    description TEXT,
    departement_id INT REFERENCES departement(id) ON DELETE CASCADE,
    grille_salariale_id INT REFERENCES grille_salariale(id) ON DELETE RESTRICT,
    statut_poste_id INT REFERENCES statut_poste(id) ON DELETE RESTRICT,
    poste_parent_id INT REFERENCES poste(id) ON DELETE SET NULL -- Nullable pour l'héritage mère-fille
);

CREATE TABLE type_demande_recrutement(
    id SERIAL PRIMARY KEY,
    libelle VARCHAR(50) NOT NULL
);

-- 3. TABLES DU WORKFLOW (MASTER-DETAIL)

CREATE TABLE demande_recrutement (
    id SERIAL PRIMARY KEY,
    id_type_demande INT REFERENCES type_demande_recrutement(id),
    demandeur_id INT REFERENCES utilisateur(id) ON DELETE CASCADE,
    departement_id INT REFERENCES departement(id) ON DELETE CASCADE,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    statut_demande_id INT REFERENCES statut_demande(id) ON DELETE RESTRICT
);

CREATE TABLE ligne_demande_recrutement (
    id SERIAL PRIMARY KEY,
    demande_recrutement_id INT REFERENCES demande_recrutement(id) ON DELETE CASCADE,
    type_besoin VARCHAR(50) NOT NULL, -- REMPLACEMENT, AUGMENTATION_EFFECTIF, CREATION
    poste_id INT REFERENCES poste(id) ON DELETE RESTRICT, -- Pointe vers le poste (existant ou créé en mode PROJET)
    nombre_postes INT NOT NULL DEFAULT 1,
    motif TEXT,
    date_essai_souhaitee DATE,
    salaire_propose NUMERIC(12, 2) NOT NULL, -- Pré-rempli par la grille mais modifiable/débattable
    statut_ligne_id INT REFERENCES statut_ligne(id) ON DELETE RESTRICT
);

CREATE TABLE historique_validation_ligne (
    id SERIAL PRIMARY KEY,
    ligne_demande_id INT REFERENCES ligne_demande_recrutement(id) ON DELETE CASCADE,
    valideur_id INT REFERENCES utilisateur(id) ON DELETE RESTRICT,
    etape VARCHAR(50) NOT NULL, -- RH, FINANCE, DG
    statut_validation_id INT REFERENCES statut_validation(id) ON DELETE RESTRICT,
    commentaire TEXT,
    date_action TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

