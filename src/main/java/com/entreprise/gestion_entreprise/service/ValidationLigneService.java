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
import java.util.stream.Collectors;

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
    private final OffreEmploiService offreEmploiService;

    public ValidationLigneService(LigneDemandeRecrutementRepository ligneRepository,
            HistoriqueValidationLigneRepository historiqueRepository,
            StatutValidationRepository statutValidationRepository,
            StatutLigneRepository statutLigneRepository, OffreEmploiService offreEmploiService) {
        this.ligneRepository = ligneRepository;
        this.historiqueRepository = historiqueRepository;
        this.statutValidationRepository = statutValidationRepository;
        this.statutLigneRepository = statutLigneRepository;
        this.offreEmploiService = offreEmploiService;
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

    @Transactional(readOnly = true)
    public long compterEnAttenteRH() {
        return getLignesEnAttenteRH().size();
    }

    @Transactional(readOnly = true)
    public long compterRefuseesRH() {
        return getLignesRefuseesParRH().size();
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

        // 3. Vérifier que la ligne peut recevoir une décision.
        if (ETAPE_RH.equals(etape)) {
            verifierAttenteRH(ligne);
        } else {
            verifierDGPeutDecider(ligne, decisionCode);
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

        // 7. Transformation en offre d'emploi (étape DG uniquement)
        if (ETAPE_DG.equals(etape)) {
            if (CODE_APPROUVEE.equals(decisionCode)) {
                offreEmploiService.creerDepuisLigne(ligne);
            } else {
                offreEmploiService.retirerOffreDeLigne(ligne);
            }
        }
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

    /**
     * Le DG peut décider sur n'importe quelle ligne, y compris une ligne refusée
     * par le RH ou la Finance. Seul garde-fou : ne pas enregistrer deux fois de
     * suite
     * la même décision DG.
     */
    private void verifierDGPeutDecider(LigneDemandeRecrutement ligne, String decisionCode) {
        List<HistoriqueValidationLigne> decisionsDG = historiqueRepository
                .findByLigneDemandeIdOrderByDateActionAsc(ligne.getId()).stream()
                .filter(h -> ETAPE_DG.equals(h.getEtape()))
                .collect(Collectors.toList());
        if (!decisionsDG.isEmpty()) {
            HistoriqueValidationLigne derniere = decisionsDG.get(decisionsDG.size() - 1);
            if (decisionCode.equals(derniere.getStatutValidation().getCode())) {
                throw new RegleGestionException(CODE_APPROUVEE.equals(decisionCode)
                        ? "Cette demande a déjà été validée par la Direction Générale."
                        : "Cette demande a déjà été refusée par la Direction Générale.");
            }
        }
    }

    private StatutLigne findStatutLigne(String code) {
        return statutLigneRepository.findByCode(code)
                .orElseThrow(() -> new RegleGestionException("Statut de ligne '" + code + "' introuvable."));
    }

    @Transactional
    public void accepterLigne(Integer ligneId, String commentaire, Utilisateur valideur) {
        validerLigne(ligneId, CODE_APPROUVEE, etapeSelonRole(valideur), commentaire, valideur);
    }

    @Transactional
    public void refuserLigne(Integer ligneId, String commentaire, Utilisateur valideur) {
        if (commentaire == null || commentaire.trim().isEmpty()) {
            throw new RegleGestionException("Un commentaire est obligatoire pour refuser une demande.");
        }
        validerLigne(ligneId, CODE_REFUSEE, etapeSelonRole(valideur), commentaire, valideur);
    }

    private String etapeSelonRole(Utilisateur valideur) {
        String roleCode = (valideur != null && valideur.getRole() != null)
                ? valideur.getRole().getCode()
                : null;
        if ("DG".equals(roleCode))
            return ETAPE_DG;
        if ("RH".equals(roleCode))
            return ETAPE_RH;
        throw new RegleGestionException("Vous n'avez pas le droit de valider une demande de recrutement.");
    }
}
