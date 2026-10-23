package com.entreprise.gestion_entreprise.portail;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.entreprise.gestion_entreprise.exception.RegleGestionException;
import com.entreprise.gestion_entreprise.model.Candidat;
import com.entreprise.gestion_entreprise.service.CandidatService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Compte candidat du portail public : inscription, connexion et déconnexion.
 * Le paramètre "retour" ramène le candidat là où il en était (par exemple le formulaire
 * de candidature) ; il n'est accepté que s'il pointe vers le portail.
 */
@Controller
@RequestMapping(CandidatSession.PORTAIL)
public class PortailCompteController {

    private final CandidatService candidatService;
    private final CandidatSession candidatSession;

    public PortailCompteController(CandidatService candidatService, CandidatSession candidatSession) {
        this.candidatService = candidatService;
        this.candidatSession = candidatSession;
    }

    // ---------- Inscription ----------

    @GetMapping("/inscription")
    public String formulaireInscription(@RequestParam(value = "retour", required = false) String retour,
            HttpServletRequest request, Model model) {
        String destination = candidatSession.retourSur(retour);
        if (candidatSession.id(request).isPresent()) {
            return "redirect:" + destination;
        }
        model.addAttribute("retour", destination);
        return "offre/candidat/inscription";
    }

    @PostMapping("/inscription")
    public String inscrire(@RequestParam(value = "nom", required = false) String nom,
            @RequestParam(value = "prenom", required = false) String prenom,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "motDePasse", required = false) String motDePasse,
            @RequestParam(value = "confirmation", required = false) String confirmation,
            @RequestParam(value = "retour", required = false) String retour,
            HttpServletRequest request, Model model) {
        String destination = candidatSession.retourSur(retour);
        try {
            Candidat candidat = candidatService.inscrire(nom, prenom, email, motDePasse, confirmation);
            candidatSession.ouvrir(request, candidat);
            return "redirect:" + destination;
        } catch (RegleGestionException e) {
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("retour", destination);
            model.addAttribute("nom", nom);
            model.addAttribute("prenom", prenom);
            model.addAttribute("email", email);
            return "offre/candidat/inscription";
        }
    }

    // ---------- Connexion ----------

    @GetMapping("/connexion")
    public String formulaireConnexion(@RequestParam(value = "retour", required = false) String retour,
            HttpServletRequest request, Model model) {
        String destination = candidatSession.retourSur(retour);
        if (candidatSession.id(request).isPresent()) {
            return "redirect:" + destination;
        }
        model.addAttribute("retour", destination);
        return "offre/candidat/connexion";
    }

    @PostMapping("/connexion")
    public String connecter(@RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "motDePasse", required = false) String motDePasse,
            @RequestParam(value = "retour", required = false) String retour,
            HttpServletRequest request, Model model) {
        String destination = candidatSession.retourSur(retour);
        try {
            Candidat candidat = candidatService.authentifier(email, motDePasse);
            candidatSession.ouvrir(request, candidat);
            return "redirect:" + destination;
        } catch (RegleGestionException e) {
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("retour", destination);
            model.addAttribute("email", email);
            return "offre/candidat/connexion";
        }
    }

    // ---------- Déconnexion ----------

    @PostMapping("/deconnexion")
    public String deconnecter(HttpServletRequest request) {
        candidatSession.fermer(request);
        return "redirect:" + CandidatSession.PORTAIL;
    }
}