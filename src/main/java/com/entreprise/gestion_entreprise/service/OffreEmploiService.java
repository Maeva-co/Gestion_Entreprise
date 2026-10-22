package com.entreprise.gestion_entreprise.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.entreprise.gestion_entreprise.exception.RegleGestionException;
import com.entreprise.gestion_entreprise.model.LigneDemandeRecrutement;
import com.entreprise.gestion_entreprise.model.OffreEmploi;
import com.entreprise.gestion_entreprise.model.Poste;
import com.entreprise.gestion_entreprise.model.StatutOffre;
import com.entreprise.gestion_entreprise.repository.OffreEmploiRepository;
import com.entreprise.gestion_entreprise.repository.StatutOffreRepository;

@Service
public class OffreEmploiService {

    public static final String CODE_OFFRE_BROUILLON = "BROUILLON";
    public static final String CODE_OFFRE_PUBLIEE = "PUBLIEE";
    public static final String CODE_OFFRE_SUSPENDUE = "SUSPENDUE";
    public static final String CODE_OFFRE_CLOTUREE = "CLOTUREE";

    /** Codes de statut connus, dans l'ordre d'affichage des filtres. */
    public static final List<String> CODES_STATUT = List.of(
            CODE_OFFRE_BROUILLON, CODE_OFFRE_PUBLIEE, CODE_OFFRE_SUSPENDUE, CODE_OFFRE_CLOTUREE);

    /**
     * Changements de statut autorisés : statut actuel -> statuts proposables.
     * CLOTUREE est un état final.
     */
    public static final Map<String, List<String>> TRANSITIONS = Map.of(
            CODE_OFFRE_BROUILLON, List.of(CODE_OFFRE_PUBLIEE),
            CODE_OFFRE_PUBLIEE, List.of(CODE_OFFRE_SUSPENDUE, CODE_OFFRE_CLOTUREE),
            CODE_OFFRE_SUSPENDUE, List.of(CODE_OFFRE_PUBLIEE, CODE_OFFRE_CLOTUREE),
            CODE_OFFRE_CLOTUREE, List.of());

    private final OffreEmploiRepository offreRepository;
    private final StatutOffreRepository statutOffreRepository;

    public OffreEmploiService(OffreEmploiRepository offreRepository,
            StatutOffreRepository statutOffreRepository) {
        this.offreRepository = offreRepository;
        this.statutOffreRepository = statutOffreRepository;
    }

    // ---------- Consultation (espace RH) ----------

    /**
     * Liste des offres, de la plus récente à la plus ancienne.
     * Si le code de statut est nul ou inconnu, toutes les offres sont retournées.
     */
    @Transactional(readOnly = true)
    public List<OffreEmploi> lister(String codeStatut) {
        if (codeStatut != null && CODES_STATUT.contains(codeStatut)) {
            return offreRepository.findByStatutAvecDetails(codeStatut);
        }
        return offreRepository.findAllAvecDetails();
    }

    /** Nombre d'offres par statut. Tous les statuts sont présents, même à 0. */
    @Transactional(readOnly = true)
    public Map<String, Long> compterParStatut() {
        Map<String, Long> compteurs = new LinkedHashMap<>();
        for (String code : CODES_STATUT) {
            compteurs.put(code, 0L);
        }
        for (Object[] ligne : offreRepository.compterParStatut()) {
            compteurs.put((String) ligne[0], (Long) ligne[1]);
        }
        return compteurs;
    }

    @Transactional(readOnly = true)
    public long compterToutes() {
        return offreRepository.count();
    }

    /** Statuts vers lesquels une offre peut passer depuis le statut donné. */
    public static List<String> transitionsDepuis(String codeStatut) {
        return TRANSITIONS.getOrDefault(codeStatut, List.of());
    }

    // ---------- Changement de statut (espace RH) ----------

    /**
     * Change le statut d'une offre en respectant les transitions autorisées.
     * - PUBLIEE : renseigne la date de première publication (conservée si l'offre
     * est suspendue puis republiée).
     * - CLOTUREE : renseigne la date de clôture.
     */
    @Transactional
    public OffreEmploi changerStatut(Integer offreId, String nouveauCode) {
        if (nouveauCode == null || !CODES_STATUT.contains(nouveauCode)) {
            throw new RegleGestionException("Statut d'offre inconnu.");
        }

        OffreEmploi offre = offreRepository.findById(offreId)
                .orElseThrow(() -> new RegleGestionException("Offre d'emploi introuvable (id " + offreId + ")."));

        String codeActuel = offre.getStatutOffre().getCode();
        if (codeActuel.equals(nouveauCode)) {
            throw new RegleGestionException("L'offre a déjà ce statut.");
        }
        if (!transitionsDepuis(codeActuel).contains(nouveauCode)) {
            throw new RegleGestionException("Changement de statut non autorisé : "
                    + codeActuel + " → " + nouveauCode + ".");
        }

        StatutOffre nouveauStatut = statutOffreRepository.findByCode(nouveauCode)
                .orElseThrow(() -> new RegleGestionException(
                        "Statut d'offre '" + nouveauCode + "' introuvable."));

        LocalDateTime maintenant = LocalDateTime.now();
        if (CODE_OFFRE_PUBLIEE.equals(nouveauCode) && offre.getDatePublication() == null) {
            offre.setDatePublication(maintenant);
        }
        if (CODE_OFFRE_CLOTUREE.equals(nouveauCode)) {
            offre.setDateCloture(maintenant);
        }
        offre.setStatutOffre(nouveauStatut);
        return offreRepository.save(offre);
    }

    // ---------- Transformation des demandes validées par le DG ----------

    /**
     * Transforme une ligne validée par le DG en offre d'emploi (statut BROUILLON).
     * Idempotent : si l'offre existe déjà (le DG a validé, refusé puis revalidé),
     * elle est simplement retournée.
     * À appeler dans la même transaction que la décision DG.
     */
    @Transactional
    public OffreEmploi creerDepuisLigne(LigneDemandeRecrutement ligne) {
        return offreRepository.findByLigneDemandeId(ligne.getId()).orElseGet(() -> {
            StatutOffre brouillon = statutOffreRepository.findByCode(CODE_OFFRE_BROUILLON)
                    .orElseThrow(() -> new RegleGestionException(
                            "Statut d'offre '" + CODE_OFFRE_BROUILLON + "' introuvable."));

            Poste poste = ligne.getPoste();

            OffreEmploi offre = new OffreEmploi();
            offre.setLigneDemande(ligne);
            offre.setTitreOffre(poste.getTitre());
            offre.setDescriptionOffre(poste.getDescription());
            offre.setStatutOffre(brouillon);
            return offreRepository.save(offre);
        });
    }

    /**
     * Appelé quand le DG refuse une ligne : une offre encore en BROUILLON est
     * supprimée.
     * Si l'offre a déjà été publiée (ou clôturée), le refus est bloqué.
     */
    @Transactional
    public void retirerOffreDeLigne(LigneDemandeRecrutement ligne) {
        offreRepository.findByLigneDemandeId(ligne.getId()).ifPresent(offre -> {
            if (!CODE_OFFRE_BROUILLON.equals(offre.getStatutOffre().getCode())) {
                throw new RegleGestionException(
                        "Une offre d'emploi a déjà été publiée pour cette demande. "
                                + "Clôturez-la avant de refuser la demande.");
            }
            offreRepository.delete(offre);
        });
    }

}