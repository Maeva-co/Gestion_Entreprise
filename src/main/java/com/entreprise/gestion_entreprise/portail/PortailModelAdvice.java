package com.entreprise.gestion_entreprise.portail;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.entreprise.gestion_entreprise.model.Candidat;
import com.entreprise.gestion_entreprise.service.CandidatService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Données communes aux pages du portail candidat (package portail uniquement) :
 * le candidat connecté, ou null pour un visiteur.
 * Séparé de UtilisateurModelAdvice, qui concerne le personnel (RH, DG, Finance).
 */
@ControllerAdvice(basePackages = "com.entreprise.gestion_entreprise.portail")
public class PortailModelAdvice {

    private final CandidatSession candidatSession;
    private final CandidatService candidatService;

    public PortailModelAdvice(CandidatSession candidatSession, CandidatService candidatService) {
        this.candidatSession = candidatSession;
        this.candidatService = candidatService;
    }

    @ModelAttribute("candidatConnecte")
    public Candidat candidatConnecte(HttpServletRequest request) {
        return candidatSession.id(request).flatMap(candidatService::trouver).orElse(null);
    }
}