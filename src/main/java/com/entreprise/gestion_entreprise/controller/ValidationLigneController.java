package com.entreprise.gestion_entreprise.controller;

import com.entreprise.gestion_entreprise.dto.StatutSuivi;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

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

    // @PostMapping("/rh/lignes/{id}/accepter")
    // public String accepterLigneRH(@PathVariable Integer id,
    //         @RequestParam(value = "commentaire", required = false) String commentaire,
    //         Model model,
    //         @ModelAttribute(value = "utilisateur", binding = false) Utilisateur utilisateur) {
    //     try {
    //         validationService.accepterLigneRH(id, commentaire, utilisateur);
    //         model.addAttribute("message", "La demande a été acceptée et transmise à la Direction Générale.");
    //         model.addAttribute("messageType", "success");
    //     } catch (RegleGestionException e) {
    //         model.addAttribute("message", e.getMessage());
    //         model.addAttribute("messageType", "error");
    //     }
    //     return viewListeRH(model);
    // }

    // @PostMapping("/rh/lignes/{id}/refuser")
    // public String refuserLigneRH(@PathVariable Integer id,
    //         @RequestParam("commentaire") String commentaire,
    //         Model model,
    //         @ModelAttribute(value = "utilisateur", binding = false) Utilisateur utilisateur) {
    //     try {
    //         validationService.refuserLigneRH(id, commentaire, utilisateur);
    //         model.addAttribute("message", "La demande a été refusée.");
    //         model.addAttribute("messageType", "success");
    //     } catch (RegleGestionException e) {
    //         model.addAttribute("message", e.getMessage());
    //         model.addAttribute("messageType", "error");
    //     }
    //     return viewListeRH(model);
    // }

    @GetMapping("/rh/refusees")
    public String listeRefuseesRH(Model model) {
        model.addAttribute("lignes", validationService.getLignesRefuseesParRH());
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

    @PostMapping("/lignes/{id}/accepter")
    public String accepterLigne(@PathVariable Integer id,
            @RequestParam(value = "commentaire", required = false) String commentaire,
            @RequestParam(value = "retour", defaultValue = "liste") String retour,
            RedirectAttributes redirectAttributes,
            @ModelAttribute(value = "utilisateur", binding = false) Utilisateur utilisateur) {
        try {
            StatutSuivi etat = validationService.accepterLigne(id, commentaire, utilisateur);
            redirectAttributes.addFlashAttribute("message", messageApresAcceptation(etat));
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (RegleGestionException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return redirectVers(retour);
    }

    @PostMapping("/lignes/{id}/refuser")
    public String refuserLigne(@PathVariable Integer id,
            @RequestParam("commentaire") String commentaire,
            @RequestParam(value = "retour", defaultValue = "liste") String retour,
            RedirectAttributes redirectAttributes,
            @ModelAttribute(value = "utilisateur", binding = false) Utilisateur utilisateur) {
        try {
            validationService.refuserLigne(id, commentaire, utilisateur);
            redirectAttributes.addFlashAttribute("message", "La demande a été refusée.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (RegleGestionException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return redirectVers(retour);
    }

    private static final Map<String, String> RETOURS = Map.of(
            "liste", "redirect:/demandes-recrutement/validation/rh/liste",
            "refusees", "redirect:/demandes-recrutement/validation/rh/refusees");

    private String redirectVers(String retour) {
        return RETOURS.getOrDefault(retour, RETOURS.get("liste"));
    }

    // ---------- Helpers ----------

    private String viewListeRH(Model model) {
        model.addAttribute("lignes", validationService.getLignesEnAttenteRH());
        model.addAttribute("page", "rh-liste");
        return "demande-recrutement/rh-liste";
    }

    private static final String ETAPE_DG = "DG";

    private String viewListeDG(Model model) {
        List<LigneDemandeRecrutement> lignes = validationService.getLignesEnAttente(ETAPE_DG);
        model.addAttribute("lignes", lignes);
        model.addAttribute("nombreEnAttente", lignes.size());
        return "demande-recrutement/dg-liste";
    }

    private String messageApresAcceptation(StatutSuivi etat) {
        return etat.getType() == StatutSuivi.Type.VALIDEE
                ? "La demande est validée à toutes les étapes : elle devient une offre d'emploi."
                : "La demande a été acceptée et transmise à l'étape « " + etat.getEtapeLibelle() + " ».";
    }
}
