package com.entreprise.gestion_entreprise.service;

import com.entreprise.gestion_entreprise.config.CircuitValidation;
import com.entreprise.gestion_entreprise.config.EtapeValidation;
import com.entreprise.gestion_entreprise.dto.LigneSuivi;
import com.entreprise.gestion_entreprise.dto.StatutSuivi;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Validation des lignes de recrutement selon le circuit configuré dans
 * {@link CircuitValidation}. L'avancement d'une ligne est toujours déduit de
 * l'historique des décisions (source de vérité) ; le statut de la ligne n'en
 * est
 * que le reflet : EN_ATTENTE tant que le circuit n'est pas terminé, VALIDEE
 * quand
 * toutes les étapes ont approuvé, REFUSEE après un refus.
 */
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

    /**
     * Étape des pages "RH" existantes (en attente, refusées). Pour les autres
     * étapes, utiliser getLignesEnAttente(code) et getLignesRefuseesPar(code).
     */
    private static final String ETAPE_RH = "RH";

    private final LigneDemandeRecrutementRepository ligneRepository;
    private final HistoriqueValidationLigneRepository historiqueRepository;
    private final StatutValidationRepository statutValidationRepository;
    private final StatutLigneRepository statutLigneRepository;
    private final OffreEmploiService offreEmploiService;
    private final CircuitValidation circuit;

    public ValidationLigneService(LigneDemandeRecrutementRepository ligneRepository,
            HistoriqueValidationLigneRepository historiqueRepository,
            StatutValidationRepository statutValidationRepository,
            StatutLigneRepository statutLigneRepository, OffreEmploiService offreEmploiService,
            CircuitValidation circuit) {
        this.ligneRepository = ligneRepository;
        this.historiqueRepository = historiqueRepository;
        this.statutValidationRepository = statutValidationRepository;
        this.statutLigneRepository = statutLigneRepository;
        this.offreEmploiService = offreEmploiService;
        this.circuit = circuit;
    }

    // ---------- Lectures ----------

    /**
     * Toutes les lignes de recrutement (de la plus récente à la plus ancienne),
     * chacune avec son étape d'avancement dans le circuit.
     * L'historique est lu en une seule requête pour toutes les lignes.
     */
    @Transactional(readOnly = true)
    public List<LigneSuivi> getSuiviLignes() {
        List<LigneDemandeRecrutement> lignes = ligneRepository.findAllPourSuivi();
        if (lignes.isEmpty()) {
            return List.of();
        }

        List<Integer> ids = lignes.stream().map(LigneDemandeRecrutement::getId).toList();
        Map<Integer, List<HistoriqueValidationLigne>> historiqueParLigne = historiqueRepository
                .findByLigneDemandeIdInOrderByIdAsc(ids).stream()
                .collect(Collectors.groupingBy(h -> h.getLigneDemande().getId()));

        return lignes.stream()
                .map(l -> new LigneSuivi(l, calculerEtat(historiqueParLigne.getOrDefault(l.getId(), List.of()))))
                .toList();
    }

    /** Lignes qui attendent la décision de l'étape donnée. */
    @Transactional(readOnly = true)
    public List<LigneDemandeRecrutement> getLignesEnAttente(String codeEtape) {
        return getSuiviLignes().stream()
                .filter(l -> l.getStatut().attend(codeEtape))
                .map(LigneSuivi::getLigne)
                .toList();
    }

    /**
     * Lignes refusées par l'étape donnée (et pas encore tranchées par l'étape
     * décisive).
     */
    @Transactional(readOnly = true)
    public List<LigneDemandeRecrutement> getLignesRefuseesPar(String codeEtape) {
        return getSuiviLignes().stream()
                .filter(l -> l.getStatut().estRefuseePar(codeEtape))
                .map(LigneSuivi::getLigne)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LigneDemandeRecrutement> getLignesEnAttenteRH() {
        return getLignesEnAttente(ETAPE_RH);
    }

    @Transactional(readOnly = true)
    public List<LigneDemandeRecrutement> getLignesRefuseesParRH() {
        return getLignesRefuseesPar(ETAPE_RH);
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

    // ---------- Décisions ----------

    /**
     * Accepte une ligne à l'étape que le rôle du valideur permet de valider.
     * Retourne le nouvel état de la ligne (prochaine étape, ou validée).
     */
    @Transactional
    public StatutSuivi accepterLigne(Integer ligneId, String commentaire, Utilisateur valideur) {
        return decider(ligneId, CODE_APPROUVEE, etapeSelonRole(valideur), commentaire, valideur);
    }

    /** Refuse une ligne ; un commentaire est obligatoire. */
    @Transactional
    public StatutSuivi refuserLigne(Integer ligneId, String commentaire, Utilisateur valideur) {
        if (commentaire == null || commentaire.trim().isEmpty()) {
            throw new RegleGestionException("Un commentaire est obligatoire pour refuser une demande.");
        }
        return decider(ligneId, CODE_REFUSEE, etapeSelonRole(valideur), commentaire, valideur);
    }

    // ---------- Cœur métier ----------

    /**
     * Enregistre une décision à une étape du circuit et met à jour le statut de la
     * ligne. Appelée uniquement depuis les méthodes publiques transactionnelles.
     */
    private StatutSuivi decider(Integer ligneId, String decisionCode, EtapeValidation etape,
            String commentaire, Utilisateur valideur) {

        // 1. Verrou pessimiste sur la ligne pour éviter les décisions concurrentes.
        LigneDemandeRecrutement ligne = ligneRepository.findByIdForUpdate(ligneId)
                .orElseThrow(() -> new RegleGestionException("Ligne de recrutement introuvable (id " + ligneId + ")."));

        // 2. Le rôle du valideur doit correspondre à l'étape.
        String roleCode = (valideur != null && valideur.getRole() != null) ? valideur.getRole().getCode() : null;
        if (!etape.getRoleCode().equals(roleCode)) {
            throw new RegleGestionException(
                    "Vous n'avez pas le droit de valider à l'étape " + etape.getLibelle() + ".");
        }

        // 3. La ligne doit pouvoir recevoir une décision à cette étape.
        List<HistoriqueValidationLigne> historique = new ArrayList<>(
                historiqueRepository.findByLigneDemandeIdOrderByDateActionAsc(ligne.getId()));
        verifierPeutDecider(etape, decisionCode, calculerEtat(historique), historique);

        // 4. Statut de validation correspondant à la décision.
        StatutValidation statut = statutValidationRepository.findByCode(decisionCode)
                .orElseThrow(
                        () -> new RegleGestionException("Statut de validation '" + decisionCode + "' introuvable."));

        // 5. Enregistrer la décision dans l'historique (jamais de modification
        // d'entrées existantes).
        HistoriqueValidationLigne decision = new HistoriqueValidationLigne();
        decision.setLigneDemande(ligne);
        decision.setValideur(valideur);
        decision.setEtape(etape.getCode());
        decision.setStatutValidation(statut);
        decision.setCommentaire(commentaire);
        // date_action est renseignée automatiquement via @PrePersist.
        historiqueRepository.save(decision);
        historique.add(decision);

        // 6. Le statut de la ligne reflète le nouvel état du circuit.
        StatutSuivi nouvelEtat = calculerEtat(historique);
        ligne.setStatutLigne(findStatutLigne(codeStatutLigne(nouvelEtat)));
        ligneRepository.save(ligne);

        // 7. L'étape décisive transforme la ligne en offre d'emploi (ou la retire).
        if (circuit.estDecisive(etape.getCode())) {
            if (CODE_APPROUVEE.equals(decisionCode)) {
                offreEmploiService.creerDepuisLigne(ligne);
            } else {
                offreEmploiService.retirerOffreDeLigne(ligne);
            }
        }
        return nouvelEtat;
    }

    /**
     * Où en est la ligne, déduit de l'historique (décisions APPROUVEE ou REFUSEE
     * uniquement ; les entrées "en attente" ne comptent pas).
     * - L'étape décisive a le dernier mot : sa décision l'emporte sur tout refus
     * antérieur.
     * - Sinon, on parcourt le circuit dans l'ordre : la première étape sans
     * décision
     * est celle qui est attendue, la première étape qui a refusé bloque la ligne.
     */
    private StatutSuivi calculerEtat(List<HistoriqueValidationLigne> historique) {
        EtapeValidation decisive = circuit.getEtapeDecisive();
        String decisionDecisive = derniereDecision(historique, decisive.getCode());
        if (decisionDecisive != null) {
            return CODE_APPROUVEE.equals(decisionDecisive) ? StatutSuivi.validee() : StatutSuivi.refusee(decisive);
        }

        for (EtapeValidation etape : circuit.getEtapes()) {
            String decision = derniereDecision(historique, etape.getCode());
            if (decision == null) {
                return StatutSuivi.aValider(etape);
            }
            if (CODE_REFUSEE.equals(decision)) {
                return StatutSuivi.refusee(etape);
            }
        }
        return StatutSuivi.validee();
    }

    /**
     * Dernière décision (APPROUVEE ou REFUSEE) enregistrée pour une étape, ou null.
     */
    private String derniereDecision(List<HistoriqueValidationLigne> historique, String codeEtape) {
        String derniere = null;
        for (HistoriqueValidationLigne h : historique) {
            if (!codeEtape.equals(h.getEtape())) {
                continue;
            }
            String code = h.getStatutValidation().getCode();
            if (CODE_APPROUVEE.equals(code) || CODE_REFUSEE.equals(code)) {
                derniere = code;
            }
        }
        return derniere;
    }

    /**
     * Une étape ne décide que lorsque la ligne l'attend (toutes les précédentes ont
     * approuvé). L'étape décisive peut en plus trancher une ligne refusée plus tôt,
     * ou changer sa propre décision ; seul garde-fou : ne pas enregistrer deux fois
     * de suite la même décision.
     */
    private void verifierPeutDecider(EtapeValidation etape, String decisionCode,
            StatutSuivi etat, List<HistoriqueValidationLigne> historique) {

        if (circuit.estDecisive(etape.getCode())) {
            String derniere = derniereDecision(historique, etape.getCode());
            if (decisionCode.equals(derniere)) {
                throw new RegleGestionException(CODE_APPROUVEE.equals(decisionCode)
                        ? "Cette demande a déjà été validée à l'étape " + etape.getLibelle() + "."
                        : "Cette demande a déjà été refusée à l'étape " + etape.getLibelle() + ".");
            }
            return;
        }

        if (!etat.attend(etape.getCode())) {
            throw new RegleGestionException(
                    "Cette ligne n'attend pas de décision de l'étape " + etape.getLibelle() + ".");
        }
    }

    private String codeStatutLigne(StatutSuivi etat) {
        return switch (etat.getType()) {
            case VALIDEE -> CODE_LIGNE_VALIDEE;
            case REFUSEE -> CODE_LIGNE_REFUSEE;
            case A_VALIDER -> CODE_LIGNE_EN_ATTENTE;
        };
    }

    private StatutLigne findStatutLigne(String code) {
        return statutLigneRepository.findByCode(code)
                .orElseThrow(() -> new RegleGestionException("Statut de ligne '" + code + "' introuvable."));
    }

    private EtapeValidation etapeSelonRole(Utilisateur valideur) {
        String roleCode = (valideur != null && valideur.getRole() != null)
                ? valideur.getRole().getCode()
                : null;
        return circuit.parRole(roleCode).orElseThrow(
                () -> new RegleGestionException("Vous n'avez pas le droit de valider une demande de recrutement."));
    }
}