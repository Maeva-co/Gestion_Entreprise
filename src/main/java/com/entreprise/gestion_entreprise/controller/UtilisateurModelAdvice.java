package com.entreprise.gestion_entreprise.controller;

import java.security.Principal;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.entreprise.gestion_entreprise.model.Utilisateur;
import com.entreprise.gestion_entreprise.repository.UtilisateurRepository;
import com.entreprise.gestion_entreprise.service.ValidationLigneService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Données communes aux pages qui affichent le menu latéral :
 * utilisateur connecté, URI courante (item actif) et compteurs des badges.
 * Si une autre page utilise le fragment sidebar, ajouter son contrôleur
 * dans assignableTypes.
 */
@ControllerAdvice(assignableTypes = {
        ValidationLigneController.class,
        DemandeRecrutementController.class,
        OffreController.class })
public class UtilisateurModelAdvice {

    private final UtilisateurRepository utilisateurRepository;
    private final ValidationLigneService validationService;
    // TODO : injecter ton service / repository de demandes
    // private final DemandeRecrutementRepository demandeRepository;

    public UtilisateurModelAdvice(UtilisateurRepository utilisateurRepository,
            ValidationLigneService validationService) {
        this.utilisateurRepository = utilisateurRepository;
        this.validationService = validationService;
    }

    @ModelAttribute("utilisateur")
    public Utilisateur utilisateur(Principal principal) {
        if (principal == null)
            return null;
        return utilisateurRepository.findByEmail(principal.getName()).orElse(null);
    }

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        return (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx))
                ? uri.substring(ctx.length())
                : uri;
    }

    @ModelAttribute("nombreEnAttente")
    public long nombreEnAttente() {
        return validationService.compterEnAttenteRH();
    }

    @ModelAttribute("nombreRefusees")
    public long nombreRefusees() {
        return validationService.compterRefuseesRH();
    }
}