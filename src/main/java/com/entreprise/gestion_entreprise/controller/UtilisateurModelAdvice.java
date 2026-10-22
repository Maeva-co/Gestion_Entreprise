package com.entreprise.gestion_entreprise.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.entreprise.gestion_entreprise.dto.LigneSuivi;
import com.entreprise.gestion_entreprise.dto.StatutSuivi;
import com.entreprise.gestion_entreprise.model.Utilisateur;
import com.entreprise.gestion_entreprise.repository.UtilisateurRepository;
import com.entreprise.gestion_entreprise.service.ValidationLigneService;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Données communes aux pages qui affichent le menu latéral :
 * utilisateur connecté, URI courante (item actif) et compteurs des badges.
 * Si une autre page utilise le fragment sidebar, ajouter son contrôleur
 * dans assignableTypes.
 */
@ControllerAdvice(basePackages = "com.entreprise.gestion_entreprise.controller")
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

        @ModelAttribute
    public void compteursMenu(Model model) {
        Map<String, Long> enAttente = new HashMap<>();
        Map<String, Long> refusees = new HashMap<>();
 
        for (LigneSuivi ligne : validationService.getSuiviLignes()) {
            StatutSuivi statut = ligne.getStatut();
            if (statut.getType() == StatutSuivi.Type.A_VALIDER) {
                enAttente.merge(statut.getEtapeCode(), 1L, Long::sum);
            } else if (statut.estRefusee()) {
                refusees.merge(statut.getEtapeCode(), 1L, Long::sum);
            }
        }
        model.addAttribute("enAttenteParEtape", enAttente);
        model.addAttribute("refuseesParEtape", refusees);
    }

}