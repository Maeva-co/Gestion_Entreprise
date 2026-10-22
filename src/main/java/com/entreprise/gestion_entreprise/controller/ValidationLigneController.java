package com.entreprise.gestion_entreprise.controller;

import com.entreprise.gestion_entreprise.exception.RegleGestionException;
import com.entreprise.gestion_entreprise.model.HistoriqueValidationLigne;
import com.entreprise.gestion_entreprise.model.LigneDemandeRecrutement;
import com.entreprise.gestion_entreprise.model.Utilisateur;
import com.entreprise.gestion_entreprise.repository.UtilisateurRepository;
import com.entreprise.gestion_entreprise.service.AuthService;
import com.entreprise.gestion_entreprise.service.ValidationLigneService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@Controller
@RequestMapping("/demandes-recrutement/validation")
public class ValidationLigneController {

    private final ValidationLigneService validationService;

    public ValidationLigneController(ValidationLigneService validationService) {
        this.validationService = validationService;
    }

    // ---------- RH ----------

    
    @GetMapping("/rh/liste")
    public String listeRH(Model model) {
        return viewListeRH(model);
    }
 
    @GetMapping("/rh/lignes/{id}")
    public String detailLigneRH(@PathVariable Integer id, Model model) {
        model.addAttribute("ligne", validationService.getLigne(id));
        model.addAttribute("historique", validationService.getHistorique(id));
        model.addAttribute("page", "rh-detail");
        return "demande-recrutement/ligne-details";
    }
 
    @PostMapping("/rh/lignes/{id}/accepter")
    public String accepterLigneRH(@PathVariable Integer id,
            @RequestParam(value = "commentaire", required = false) String commentaire,
            Model model,
            @ModelAttribute(value = "utilisateur", binding = false) Utilisateur utilisateur) {
        try {
            validationService.accepterLigneRH(id, commentaire, utilisateur);
            model.addAttribute("message", "La demande a été acceptée et transmise à la Direction Générale.");
            model.addAttribute("messageType", "success");
        } catch (RegleGestionException e) {
            model.addAttribute("message", e.getMessage());
            model.addAttribute("messageType", "error");
        }
        return viewListeRH(model);
    }
 
    @PostMapping("/rh/lignes/{id}/refuser")
    public String refuserLigneRH(@PathVariable Integer id,
            @RequestParam("commentaire") String commentaire,
            Model model,
            @ModelAttribute(value = "utilisateur", binding = false) Utilisateur utilisateur) {
        try {
            validationService.refuserLigneRH(id, commentaire, utilisateur);
            model.addAttribute("message", "La demande a été refusée.");
            model.addAttribute("messageType", "success");
        } catch (RegleGestionException e) {
            model.addAttribute("message", e.getMessage());
            model.addAttribute("messageType", "error");
        }
        return viewListeRH(model);
    }
 
    @GetMapping("/rh/refusees")
    public String listeRefuseesRH(Model model) {
        List<LigneDemandeRecrutement> lignes = validationService.getLignesRefuseesParRH();
        model.addAttribute("lignes", lignes);
        model.addAttribute("nombreRefusees", lignes.size());
        model.addAttribute("nombreEnAttente", validationService.getLignesEnAttenteRH().size());
        model.addAttribute("page", "rh-refusees");
        return "demande-recrutement/rh-refusees";
    }
 
    @GetMapping("/rh/lignes/{id}/historique")
    public String historiqueLigne(@PathVariable Integer id, Model model) {
        model.addAttribute("ligne", validationService.getLigne(id));
        model.addAttribute("historique", validationService.getHistorique(id));
        model.addAttribute("page", "rh-historique");
        return "demande-recrutement/ligne-historique";
    }
 
    // ---------- DG ----------
 
    @GetMapping("/dg/liste")
    public String listeDG(Model model) {
        return viewListeDG(model);
    }
 
    @PostMapping("/dg/lignes/{id}/accepter")
    public String accepterLigneDG(@PathVariable Integer id,
            @RequestParam(value = "commentaire", required = false) String commentaire,
            Model model,
            @ModelAttribute(value = "utilisateur", binding = false) Utilisateur utilisateur) {
        try {
            validationService.accepterLigneDG(id, commentaire, utilisateur);
            model.addAttribute("message", "La demande a été validée par la Direction Générale.");
            model.addAttribute("messageType", "success");
        } catch (RegleGestionException e) {
            model.addAttribute("message", e.getMessage());
            model.addAttribute("messageType", "error");
        }
        return viewListeDG(model);
    }
 
    @PostMapping("/dg/lignes/{id}/refuser")
    public String refuserLigneDG(@PathVariable Integer id,
            @RequestParam("commentaire") String commentaire,
            Model model,
            @ModelAttribute(value = "utilisateur", binding = false) Utilisateur utilisateur) {
        try {
            validationService.refuserLigneDG(id, commentaire, utilisateur);
            model.addAttribute("message", "La demande a été refusée par la Direction Générale.");
            model.addAttribute("messageType", "success");
        } catch (RegleGestionException e) {
            model.addAttribute("message", e.getMessage());
            model.addAttribute("messageType", "error");
        }
        return viewListeDG(model);
    }
 
    @GetMapping("/dg/refusees-rh")
    public String listeRefuseesRHPourDG(Model model) {
        List<LigneDemandeRecrutement> lignes = validationService.getLignesRefuseesParRH();
        model.addAttribute("lignes", lignes);
        model.addAttribute("nombreRefusees", lignes.size());
        model.addAttribute("page", "dg-refusees-rh");
        return "demande-recrutement/dg-refusees-rh";
    }
 
    // ---------- Helpers ----------
 
    private String viewListeRH(Model model) {
        List<LigneDemandeRecrutement> lignes = validationService.getLignesEnAttenteRH();
        model.addAttribute("lignes", lignes);
        model.addAttribute("nombreEnAttente", lignes.size());
        model.addAttribute("nombreRefusees", validationService.getLignesRefuseesParRH().size());
        model.addAttribute("page", "rh-liste");
        return "demande-recrutement/rh-liste";
    }
 
    private String viewListeDG(Model model) {
        List<LigneDemandeRecrutement> lignes = validationService.getLignesApprouveesParRHEnAttenteDG();
        model.addAttribute("lignes", lignes);
        model.addAttribute("nombreEnAttente", lignes.size());
        model.addAttribute("page", "dg-liste");
        return "demande-recrutement/dg-liste";
    }
}