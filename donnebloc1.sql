
    
  
-- ==========================================
-- DONNEES DE TEST : BLOC 1
-- Exécuter après bloc1.sql
-- Les mots de passe sont fictifs et réservés aux tests.
-- ==========================================

BEGIN;

-- 1. Rôles
INSERT INTO role (code, libelle) VALUES
('ADMIN', 'Administrateur'),
('RH', 'Responsable ressources humaines'),
('CHEF_DEPARTEMENT', 'Chef de département'),
('FINANCE', 'Responsable financier'),
('DG', 'Directeur général');

-- 2. Départements
INSERT INTO departement (nom, description) VALUES
('Ressources humaines', 'Gestion du personnel et des recrutements'),
('Informatique', 'Développement, systèmes et support informatique'),
('Finance', 'Comptabilité, budget et contrôle financier'),
('Commercial', 'Ventes et relations clients'),
('Production', 'Opérations et suivi de la production');

-- 3. Grilles salariales
INSERT INTO grille_salariale (niveau, salaire_embauche, salaire_anciennete, salaire_revalorise) VALUES
('N1', 600000, 650000, 700000),
('N2', 800000, 900000, 1000000),
('N3', 1200000, 1350000, 1500000),
('N4', 1800000, 2000000, 2200000),
('N5', 2500000, 2800000, 3100000);

-- 4. Statuts de poste
INSERT INTO statut_poste (code, libelle) VALUES
('ACTIF', 'Poste actif'),
('PROJET', 'Poste en projet'),
('SUSPENDU', 'Poste suspendu'),
('FERME', 'Poste fermé');

-- 5. Statuts de demande
INSERT INTO statut_demande (code, libelle) VALUES
('BROUILLON', 'Brouillon'),
('EN_ATTENTE', 'En attente de validation'),
('EN_COURS', 'Validation en cours'),
('ACCEPTEE', 'Acceptée'),
('REFUSEE', 'Refusée');

-- 6. Statuts de ligne
INSERT INTO statut_ligne (code, libelle) VALUES
('BROUILLON', 'Brouillon'),
('EN_ATTENTE', 'En attente'),
('VALIDEE', 'Validée'),
('REFUSEE', 'Refusée');

-- 7. Statuts de validation
INSERT INTO statut_validation (code, libelle) VALUES
('EN_ATTENTE', 'En attente'),
('APPROUVEE', 'Approuvée'),
('REFUSEE', 'Refusée');

-- 8. Utilisateurs
-- Pour une application réelle, stocker uniquement des mots de passe hachés.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO utilisateur (nom, prenom, email, mot_de_passe, departement_id, role_id) VALUES
('Rakoto', 'Miora', 'miora.rakoto@entreprise.test', crypt('admin123', gen_salt('bf', 10)), 1, 1),
('Rabe', 'Hery', 'hery.rabe@entreprise.test', crypt('rh123', gen_salt('bf', 10)), 1, 2),
('Andria', 'Lova', 'lova.andria@entreprise.test', crypt('it123', gen_salt('bf', 10)), 2, 3),
('Ramanantsoa', 'Tiana', 'tiana.ramanantsoa@entreprise.test', crypt('finance123', gen_salt('bf', 10)), 3, 4),
('Randria', 'Fanja', 'fanja.randria@entreprise.test', crypt('dg123', gen_salt('bf', 10)), 1, 5),
('Rasolonjatovo', 'Toky', 'toky.rasolonjatovo@entreprise.test', crypt('commercial123', gen_salt('bf', 10)), 4, 3),
('Ravelo', 'Noro', 'noro.ravelo@entreprise.test', crypt('production123', gen_salt('bf', 10)), 5, 3);



-- 9. Postes
-- Les postes parents sont insérés avant leurs postes enfants.
INSERT INTO poste (titre, description, departement_id, grille_salariale_id, statut_poste_id, poste_parent_id) VALUES
('Responsable RH', 'Pilotage des activités RH', 1, 4, 1, NULL),             -- id 1
('Développeur logiciel', 'Développement et maintenance applicative', 2, 3, 1, NULL), -- id 2
('Administrateur systèmes', 'Gestion des serveurs et du réseau', 2, 3, 1, 2),       -- id 3
('Comptable', 'Comptabilité générale et clôtures', 3, 2, 1, NULL),                   -- id 4
('Commercial', 'Prospection et suivi des clients', 4, 2, 1, NULL),                   -- id 5
('Technicien de production', 'Suivi des opérations de production', 5, 1, 1, NULL),    -- id 6
('Assistant RH', 'Administration du personnel et dossiers RH', 1, 2, 1, 1),          -- id 7
('Développeur junior', 'Développement sous supervision', 2, 2, 1, 2),                -- id 8
('Chef de projet informatique', 'Coordination des projets IT', 2, 4, 1, 2),          -- id 9
('Analyste financier', 'Analyse budgétaire et reporting', 3, 3, 1, NULL);             -- id 10

-- 10. Types de demande : uniquement Remplacement et Creation
INSERT INTO type_demande_recrutement (libelle) VALUES
('Remplacement'),
('Creation');

-- 11. Demandes de recrutement
-- id_type_demande : 1 = Remplacement ; 2 = Creation
-- statut_demande_id : 1 Brouillon, 2 En attente, 3 En cours, 4 Acceptée, 5 Refusée
INSERT INTO demande_recrutement (id_type_demande, demandeur_id, departement_id, date_creation, statut_demande_id) VALUES
(1, 3, 2, '2026-10-01 09:15:00', 4),
(2, 3, 2, '2026-10-03 10:30:00', 3),
(1, 6, 4, '2026-10-05 14:00:00', 2),
(2, 7, 5, '2026-10-08 08:45:00', 5),
(1, 2, 1, '2026-10-10 11:20:00', 4),
(2, 4, 3, '2026-10-12 15:10:00', 1),
(1, 3, 2, '2026-10-15 09:00:00', 2),
(2, 6, 4, '2026-10-18 13:25:00', 3);

-- 12. Lignes des demandes
-- type_besoin : REMPLACEMENT, AUGMENTATION_EFFECTIF ou CREATION
-- Les postes projet peuvent être référencés comme les autres postes.
INSERT INTO ligne_demande_recrutement
(demande_recrutement_id, type_besoin, poste_id, nombre_postes, motif, salaire_propose, statut_ligne_id) VALUES
(1, 'REMPLACEMENT', 2, 1, 'Remplacer un développeur ayant quitté l’entreprise.', 1200000, 3),
(2, 'CREATION', 9, 1, 'Créer un poste pour piloter les nouveaux projets informatiques.', 1800000, 2),
(3, 'REMPLACEMENT', 5, 2, 'Remplacer deux commerciaux partis de l’entreprise.', 800000, 2),
(4, 'CREATION', 6, 2, 'Renforcer les équipes pour le nouveau volume de production.', 600000, 4),
(5, 'REMPLACEMENT', 7, 1, 'Remplacer un assistant RH muté vers un autre service.', 800000, 3),
(6, 'AUGMENTATION_EFFECTIF', 10, 1, 'Renforcer l’analyse financière et le reporting.', 1200000, 1),
(7, 'REMPLACEMENT', 3, 1, 'Remplacer un administrateur systèmes sortant.', 1200000, 2),
(8, 'CREATION', 5, 2, 'Créer deux postes commerciaux pour développer un nouveau secteur.', 800000, 2);

-- 13. Historique de validation
-- etape : RH, FINANCE ou DG
INSERT INTO historique_validation_ligne
(ligne_demande_id, valideur_id, etape, statut_validation_id, commentaire, date_action) VALUES
(1, 2, 'RH', 2, 'Profil et besoin de remplacement confirmés.', '2026-10-01 10:00:00'),
(1, 4, 'FINANCE', 2, 'Budget disponible pour le remplacement.', '2026-10-01 14:30:00'),
(1, 5, 'DG', 2, 'Demande approuvée.', '2026-10-02 09:00:00'),
(2, 2, 'RH', 2, 'Création du poste jugée pertinente.', '2026-10-03 13:00:00'),
(2, 4, 'FINANCE', 1, 'Vérification budgétaire en cours.', '2026-10-04 09:30:00'),
(3, 2, 'RH', 1, 'Demande en attente de vérification des profils.', '2026-10-05 16:00:00'),
(4, 2, 'RH', 3, 'Demande refusée : justification des effectifs insuffisante.', '2026-10-08 11:15:00'),
(5, 2, 'RH', 2, 'Remplacement approuvé par les RH.', '2026-10-10 13:00:00'),
(5, 4, 'FINANCE', 2, 'Budget validé.', '2026-10-10 15:30:00'),
(5, 5, 'DG', 2, 'Demande approuvée.', '2026-10-11 08:30:00'),
(7, 2, 'RH', 1, 'En attente de validation RH.', '2026-10-15 10:00:00'),
(8, 2, 'RH', 2, 'Besoin de recrutement confirmé.', '2026-10-18 15:00:00'),
(8, 4, 'FINANCE', 1, 'Budget en cours d’étude.', '2026-10-19 09:00:00');

COMMIT;

