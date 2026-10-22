package com.entreprise.gestion_entreprise.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.entreprise.gestion_entreprise.exception.RegleGestionException;
import com.entreprise.gestion_entreprise.model.Candidature;
import com.entreprise.gestion_entreprise.model.OffreEmploi;
import com.entreprise.gestion_entreprise.model.StatutCandidature;
import com.entreprise.gestion_entreprise.service.CandidatureService;

/**
 * Espace RH : candidatures reçues pour une offre d'emploi publiée.
 * Sous /offres/** : accès réservé au rôle RH (voir SecurityConfig).
 */
@Controller
@RequestMapping("/offres")
public class CandidatureController {

    private final CandidatureService candidatureService;

    public CandidatureController(CandidatureService candidatureService) {
        this.candidatureService = candidatureService;
    }

    @GetMapping("/{offreId}/candidatures")
    public String liste(@PathVariable Integer offreId,
            @RequestParam(value = "statut", required = false) String statut,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            OffreEmploi offre = candidatureService.trouverOffrePubliee(offreId);

            List<StatutCandidature> statuts = candidatureService.listerStatuts();
            // Filtre accepté seulement s'il correspond à un statut connu.
            String statutActif = (statut != null
                    && statuts.stream().anyMatch(s -> s.getCode().equals(statut))) ? statut : null;

            List<Candidature> candidatures = candidatureService.listerParOffre(offreId, statutActif);

            model.addAttribute("offre", offre);
            model.addAttribute("candidatures", candidatures);
            model.addAttribute("statuts", statuts);
            model.addAttribute("statutActif", statutActif);
            model.addAttribute("compteurs", candidatureService.compterParStatut(offreId));
            model.addAttribute("totalCandidatures", candidatureService.compterToutes(offreId));
            return "offre/rh/candidatures";
        } catch (RegleGestionException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/offres";
        }
    }
}