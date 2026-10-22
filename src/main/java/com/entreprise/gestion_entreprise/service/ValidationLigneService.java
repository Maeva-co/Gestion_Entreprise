package com.entreprise.gestion_entreprise.service;

import com.entreprise.gestion_entreprise.exception.RegleGestionException;
import com.entreprise.gestion_entreprise.model.HistoriqueValidationLigne;
import com.entreprise.gestion_entreprise.model.LigneDemandeRecrutement;
import com.entreprise.gestion_entreprise.model.StatutLigne;
import com.entreprise.gestion_entreprise.model.StatutValidation;
import com.entreprise.gestion_entreprise.model.Utilisateur;
import com.entreprise.gestion_entreprise.repository.HistoriqueValidationLigneRepository;
import com.entreprise.gestion_entreprise.repository.LigneDemandeRecrutementRepository;
import com.entreprise.gestion_entreprise.repository.StatutLigneRepository;
import com.entreprise.gestion_entreprise.repository.StatutValidationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
public class ValidationLigneService {

    /**
     * Codes de statut de validation réutilisés depuis la table statut_validation.
     */
    private static final String CODE_APPROUVEE = "APPROUVEE";
    private static final String CODE_REFUSEE = "REFUSEE";

    /** Codes de statut de ligne réutilisés depuis la table statut_ligne. */
    private static final String CODE_LIGNE_EN_ATTENTE = "EN_ATTENTE";
    private static final String CODE_LIGNE_VALIDEE = "VALIDEE";
    private static final String CODE_LIGNE_REFUSEE = "REFUSEE";

    /** Étapes de validation. */
    private static final String ETAPE_RH = "RH";
    private static final String ETAPE_DG = "DG";

    private final LigneDemandeRecrutementRepository ligneRepository;
    private final HistoriqueValidationLigneRepository historiqueRepository;
    private final StatutValidationRepository statutValidationRepository;
    private final StatutLigneRepository statutLigneRepository;

    public ValidationLigneService(LigneDemandeRecrutementRepository ligneRepository,
            HistoriqueValidationLigneRepository historiqueRepository,
            StatutValidationRepository statutValidationRepository,
            StatutLigneRepository statutLigneRepository) {
        this.ligneRepository = ligneRepository;
        this.historiqueRepository = historiqueRepository;
        this.statutValidationRepository = statutValidationRepository;
        this.statutLigneRepository = statutLigneRepository;
    }

    // ---------- Lectures ----------

    @Transactional(readOnly = true)
    public List<LigneDemandeRecrutement> getLignesEnAttenteRH() {
    return ligneRepository.findLignesEnAttenteValidationRH();
    }


    @Transactional(readOnly = true)
    public List<LigneDemandeRecrutement> getLignesRefuseesParRH() {
        return ligneRepository.findLignesRefuseesParRH();
    }

    @Transactional(readOnly = true)
    public List<LigneDemandeRecrutement> getLignesApprouveesParRHEnAttenteDG() {
        return ligneRepository.findLignesApprouveesParRHEnAttenteDG();
    }

    @Transactional(readOnly = true)
    public List<HistoriqueValidationLigne> getHistorique(Integer ligneId) {
        return historiqueRepository.findByLigneDemandeIdOrderByDateActionAsc(ligneId);
    }

    @Transactional(readOnly = true)
    public LigneDemandeRecrutement getLigne(Integer ligneId) {
        return ligneRepository.findById(ligneId)
                .orElseThrow(() -> new RegleGestionException("Ligne de recrutement introuvable (id " + ligneId + ")."));
    }

    // ---------- Décisions RH ----------

    /**
     * Accepte une ligne de recrutement à l'étape RH.
     * La ligne passe à l'étape DG (statut VALIDEE) et une entrée d'historique est
     * créée.
     */
    @Transactional
    public void accepterLigneRH(Integer ligneId, String commentaire, Utilisateur valideur) {
        validerLigne(ligneId, CODE_APPROUVEE, ETAPE_RH, commentaire, valideur);
    }

    /**
     * Refuse une ligne de recrutement à l'étape RH.
     * Un commentaire est obligatoire. La ligne ne passe PAS à l'étape DG.
     */
    @Transactional
    public void refuserLigneRH(Integer ligneId, String commentaire, Utilisateur valideur) {
        if (commentaire == null || commentaire.trim().isEmpty()) {
            throw new RegleGestionException("Un commentaire est obligatoire pour refuser une demande.");
        }
        validerLigne(ligneId, CODE_REFUSEE, ETAPE_RH, commentaire, valideur);
    }

    // ---------- Décisions DG ----------

    /**
     * Accepte une ligne de recrutement à l'étape DG.
     * La ligne doit avoir été acceptée par le RH au préalable.
     */
    @Transactional
    public void accepterLigneDG(Integer ligneId, String commentaire, Utilisateur valideur) {
        validerLigne(ligneId, CODE_APPROUVEE, ETAPE_DG, commentaire, valideur);
    }

    /**
     * Refuse une ligne de recrutement à l'étape DG.
     * Un commentaire est obligatoire.
     */
    @Transactional
    public void refuserLigneDG(Integer ligneId, String commentaire, Utilisateur valideur) {
        if (commentaire == null || commentaire.trim().isEmpty()) {
            throw new RegleGestionException("Un commentaire est obligatoire pour refuser une demande.");
        }
        validerLigne(ligneId, CODE_REFUSEE, ETAPE_DG, commentaire, valideur);
    }

    // ---------- Cœur métier ----------

    /**
     * Enregistre une décision de validation et met à jour le statut de la ligne.
     *
     * @param etape l'étape de validation (RH ou DG)
     */
    @Transactional
    private void validerLigne(Integer ligneId, String decisionCode, String etape,
            String commentaire, Utilisateur valideur) {

        // 1. Verrou pessimiste sur la ligne pour éviter les décisions concurrentes.
        LigneDemandeRecrutement ligne = ligneRepository.findByIdForUpdate(ligneId)
                .orElseThrow(() -> new RegleGestionException("Ligne de recrutement introuvable (id " + ligneId + ")."));

        // 2. Vérifier le rôle du valideur.
        String roleCode = valideur.getRole() != null ? valideur.getRole().getCode() : null;
        boolean estRH = "RH".equals(roleCode);
        boolean estDG = "DG".equals(roleCode);

        if (ETAPE_RH.equals(etape) && !estRH) {
            throw new RegleGestionException("Seul un responsable RH peut valider à l'étape RH.");
        }
        if (ETAPE_DG.equals(etape) && !estDG) {
            throw new RegleGestionException("Seul le Directeur Général peut valider à l'étape DG.");
        }

        // 3. Vérifier que la ligne attend effectivement une décision à cette étape.
        if (ETAPE_RH.equals(etape)) {
            verifierAttenteRH(ligne);
        } else {
            verifierAttenteDG(ligne);
        }

        // 4. Récupérer le statut de validation correspondant à la décision.
        StatutValidation statut = statutValidationRepository.findByCode(decisionCode)
                .orElseThrow(
                        () -> new RegleGestionException("Statut de validation '" + decisionCode + "' introuvable."));

        // 5. Enregistrer la décision dans l'historique (jamais de modification
        // d'entrées existantes).
        HistoriqueValidationLigne historique = new HistoriqueValidationLigne();
        historique.setLigneDemande(ligne);
        historique.setValideur(valideur);
        historique.setEtape(etape);
        historique.setStatutValidation(statut);
        historique.setCommentaire(commentaire);
        // date_action est renseignée automatiquement via @PrePersist.
        historiqueRepository.save(historique);

        // 6. Mettre à jour le statut métier de la ligne.
        StatutLigne nouveauStatutLigne;
        if (CODE_APPROUVEE.equals(decisionCode)) {
            if (ETAPE_RH.equals(etape)) {
                // Accepté par le RH → passe à l'étape DG.
                nouveauStatutLigne = findStatutLigne(CODE_LIGNE_VALIDEE);
            } else {
                // DG accepte : la ligne reste VALIDEE.
                nouveauStatutLigne = findStatutLigne(CODE_LIGNE_VALIDEE);
            }
        } else {
            // Refusé à n'importe quelle étape.
            nouveauStatutLigne = findStatutLigne(CODE_LIGNE_REFUSEE);
        }
        ligne.setStatutLigne(nouveauStatutLigne);

        ligneRepository.save(ligne);
    }

    private void verifierAttenteRH(LigneDemandeRecrutement ligne) {
        // La ligne doit être EN_ATTENTE et ne pas avoir déjà une décision RH.
        boolean enAttente = CODE_LIGNE_EN_ATTENTE.equals(ligne.getStatutLigne().getCode());
        boolean dejaDecideParRH = historiqueRepository.findByLigneDemandeIdAndEtape(ligne.getId(), ETAPE_RH)
                .stream()
                .anyMatch(h -> CODE_APPROUVEE.equals(h.getStatutValidation().getCode())
                        || CODE_REFUSEE.equals(h.getStatutValidation().getCode()));
        if (!enAttente || dejaDecideParRH) {
            throw new RegleGestionException("Cette ligne n'attend pas de décision de l'étape RH.");
        }
    }

    private void verifierAttenteDG(LigneDemandeRecrutement ligne) {
        // La ligne doit avoir été acceptée par le RH et ne pas avoir déjà une décision
        // DG.
        boolean approuveeParRH = historiqueRepository.findByLigneDemandeIdAndEtape(ligne.getId(), ETAPE_RH)
                .stream()
                .anyMatch(h -> CODE_APPROUVEE.equals(h.getStatutValidation().getCode()));
        boolean dejaDecideParDG = historiqueRepository.findByLigneDemandeIdAndEtape(ligne.getId(), ETAPE_DG)
                .stream()
                .anyMatch(h -> CODE_APPROUVEE.equals(h.getStatutValidation().getCode())
                        || CODE_REFUSEE.equals(h.getStatutValidation().getCode()));
        if (!approuveeParRH || dejaDecideParDG) {
            throw new RegleGestionException("Cette ligne n'attend pas de décision de l'étape DG.");
        }
    }

    private StatutLigne findStatutLigne(String code) {
        return statutLigneRepository.findByCode(code)
                .orElseThrow(() -> new RegleGestionException("Statut de ligne '" + code + "' introuvable."));
    }
}
