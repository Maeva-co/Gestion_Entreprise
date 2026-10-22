package com.entreprise.gestion_entreprise.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.entreprise.gestion_entreprise.exception.RegleGestionException;
import com.entreprise.gestion_entreprise.model.Candidature;
import com.entreprise.gestion_entreprise.model.OffreEmploi;
import com.entreprise.gestion_entreprise.model.StatutCandidature;
import com.entreprise.gestion_entreprise.repository.CandidatureRepository;
import com.entreprise.gestion_entreprise.repository.OffreEmploiRepository;
import com.entreprise.gestion_entreprise.repository.StatutCandidatureRepository;

@Service
public class CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final StatutCandidatureRepository statutRepository;
    private final OffreEmploiRepository offreRepository;

    public CandidatureService(CandidatureRepository candidatureRepository,
            StatutCandidatureRepository statutRepository,
            OffreEmploiRepository offreRepository) {
        this.candidatureRepository = candidatureRepository;
        this.statutRepository = statutRepository;
        this.offreRepository = offreRepository;
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
}