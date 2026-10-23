package com.entreprise.gestion_entreprise.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.entreprise.gestion_entreprise.exception.RegleGestionException;
import com.entreprise.gestion_entreprise.model.Candidat;
import com.entreprise.gestion_entreprise.model.Candidature;
import com.entreprise.gestion_entreprise.model.OffreEmploi;
import com.entreprise.gestion_entreprise.model.StatutCandidature;
import com.entreprise.gestion_entreprise.repository.CandidatRepository;
import com.entreprise.gestion_entreprise.repository.CandidatureRepository;
import com.entreprise.gestion_entreprise.repository.OffreEmploiRepository;
import com.entreprise.gestion_entreprise.repository.StatutCandidatureRepository;

@Service
public class CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final StatutCandidatureRepository statutRepository;
    private final CandidatRepository candidatRepository;
    private final OffreEmploiRepository offreRepository;

    public static final String CODE_RECUE = "RECUE";

    /**
     * Niveaux proposés dans le formulaire de candidature (facilite le filtrage par
     * critère).
     */
    public static final List<String> DIPLOMES = List.of("Bac", "Bac+2", "Bac+3", "Bac+5", "Doctorat", "Autre");

    private static final int EXPERIENCE_MAX = 60;
    /** Plus grande valeur de DECIMAL(12, 2). */
    private static final BigDecimal PRETENTION_MAX = new BigDecimal("9999999999.99");

    public CandidatureService(CandidatureRepository candidatureRepository,
            StatutCandidatureRepository statutRepository,
            OffreEmploiRepository offreRepository, CandidatRepository candidatRepository) {
        this.candidatureRepository = candidatureRepository;
        this.statutRepository = statutRepository;
        this.offreRepository = offreRepository;
        this.candidatRepository = candidatRepository;
    }

    /**
     * Offre dont on veut voir les candidatures.
     * Une offre jamais publiée (BROUILLON) ne peut pas avoir de candidatures.
     * Une offre suspendue ou clôturée reste consultable.
     */
    @Transactional(readOnly = true)
    public OffreEmploi trouverOffrePubliee(Integer offreId) {
        OffreEmploi offre = offreRepository.findByIdAvecStatut(offreId)
                .orElseThrow(() -> new RegleGestionException("Offre d'emploi introuvable (id " + offreId + ")."));
        if (offre.getDatePublication() == null) {
            throw new RegleGestionException(
                    "Cette offre n'a pas encore été publiée : elle n'a pas de candidatures.");
        }
        return offre;
    }

    /** Statuts du pipeline, dans l'ordre d'affichage. */
    @Transactional(readOnly = true)
    public List<StatutCandidature> listerStatuts() {
        return statutRepository.findAllByOrderByOrdreAsc();
    }

    /** Candidatures d'une offre ; si le code de statut est nul, toutes. */
    @Transactional(readOnly = true)
    public List<Candidature> listerParOffre(Integer offreId, String codeStatut) {
        if (codeStatut != null) {
            return candidatureRepository.findByOffreEtStatutAvecDetails(offreId, codeStatut);
        }
        return candidatureRepository.findByOffreAvecDetails(offreId);
    }

    @Transactional(readOnly = true)
    public boolean aDejaPostule(Integer offreId, Integer candidatId) {
        return candidatureRepository.existsByOffreEmploiIdAndCandidatId(offreId, candidatId);
    }

    /**
     * Nombre de candidatures par statut. Tous les statuts sont présents, même à 0.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> compterParStatut(Integer offreId) {
        Map<String, Long> compteurs = new LinkedHashMap<>();
        for (StatutCandidature statut : statutRepository.findAllByOrderByOrdreAsc()) {
            compteurs.put(statut.getCode(), 0L);
        }
        for (Object[] ligne : candidatureRepository.compterParStatut(offreId)) {
            compteurs.put((String) ligne[0], (Long) ligne[1]);
        }
        return compteurs;
    }

    @Transactional(readOnly = true)
    public long compterToutes(Integer offreId) {
        return candidatureRepository.countByOffreEmploiId(offreId);
    }

    @Transactional
    public Candidature postuler(Integer offreId, Integer candidatId, Integer anneeExperience,
            String dernierDiplome, BigDecimal pretentionSalariale) {

        OffreEmploi offre = offreRepository.findByIdAvecStatut(offreId)
                .filter(o -> OffreEmploiService.CODE_OFFRE_PUBLIEE.equals(o.getStatutOffre().getCode()))
                .orElseThrow(() -> new RegleGestionException("Cette offre n'accepte plus de candidatures."));
        Candidat candidat = candidatRepository.findById(candidatId)
                .orElseThrow(() -> new RegleGestionException("Compte candidat introuvable."));

        if (candidatureRepository.existsByOffreEmploiIdAndCandidatId(offreId, candidatId)) {
            throw new RegleGestionException("Vous avez déjà postulé à cette offre.");
        }

        if (anneeExperience == null || anneeExperience < 0 || anneeExperience > EXPERIENCE_MAX) {
            throw new RegleGestionException(
                    "Indiquez vos années d'expérience (entre 0 et " + EXPERIENCE_MAX + ").");
        }
        String diplome = dernierDiplome == null ? "" : dernierDiplome.trim();
        if (!DIPLOMES.contains(diplome)) {
            throw new RegleGestionException("Choisissez votre dernier diplôme dans la liste.");
        }
        if (pretentionSalariale != null
                && (pretentionSalariale.signum() < 0 || pretentionSalariale.compareTo(PRETENTION_MAX) > 0)) {
            throw new RegleGestionException("La prétention salariale saisie n'est pas valide.");
        }

        StatutCandidature recue = statutRepository.findByCode(CODE_RECUE)
                .orElseThrow(
                        () -> new RegleGestionException("Statut de candidature '" + CODE_RECUE + "' introuvable."));

        Candidature candidature = new Candidature();
        candidature.setOffreEmploi(offre);
        candidature.setCandidat(candidat);
        candidature.setAnneeExperience(anneeExperience);
        candidature.setDernierDiplome(diplome);
        candidature.setPretentionSalariale(pretentionSalariale);
        candidature.setStatutCandidature(recue);
        try {
            return candidatureRepository.saveAndFlush(candidature);
        } catch (DataIntegrityViolationException e) {
            // Double envoi simultané : la contrainte unique (offre, candidat) a joué.
            throw new RegleGestionException("Vous avez déjà postulé à cette offre.");
        }
    }

}