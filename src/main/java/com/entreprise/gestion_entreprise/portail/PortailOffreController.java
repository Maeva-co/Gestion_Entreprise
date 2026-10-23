package com.entreprise.gestion_entreprise.portail;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.entreprise.gestion_entreprise.exception.RegleGestionException;
import com.entreprise.gestion_entreprise.model.OffreEmploi;
import com.entreprise.gestion_entreprise.service.CandidatureService;
import com.entreprise.gestion_entreprise.service.OffreEmploiService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Portail public : offres publiées, détail d'une offre et candidature.
 * Les pages de consultation sont accessibles sans connexion ; postuler demande un compte candidat.
 * Le chemin /candidate-portal/** doit être ouvert (permitAll) dans la SecurityConfig.
 */
@Controller
@RequestMapping(CandidatSession.PORTAIL)
public class PortailOffreController {

    private final OffreEmploiService offreService;
    private final CandidatureService candidatureService;
    private final CandidatSession candidatSession;

    public PortailOffreController(OffreEmploiService offreService,
            CandidatureService candidatureService,
            CandidatSession candidatSession) {
        this.offreService = offreService;
        this.candidatureService = candidatureService;
        this.candidatSession = candidatSession;
    }

    // ---------- Consultation (sans connexion) ----------

    @GetMapping
    public String liste(@RequestParam(value = "q", required = false) String q, Model model) {
        List<OffreEmploi> offres = offreService.listerPubliees(q);
        model.addAttribute("offres", offres);
        model.addAttribute("q", q == null ? "" : q.trim());
        return "offre/candidat/liste";
    }

    @GetMapping("/offres/{id}")
    public String detail(@PathVariable Integer id, HttpServletRequest request,
            Model model, RedirectAttributes redirectAttributes) {
        try {
            OffreEmploi offre = offreService.trouverPubliee(id);
            boolean dejaPostule = candidatSession.id(request)
                    .map(candidatId -> candidatureService.aDejaPostule(id, candidatId))
                    .orElse(false);

            model.addAttribute("offre", offre);
            model.addAttribute("dejaPostule", dejaPostule);
            return "offre/candidat/detail";
        } catch (RegleGestionException e) {
            return redirigerAvecErreur(CandidatSession.PORTAIL, e.getMessage(), redirectAttributes);
        }
    }

    // ---------- Candidature (compte candidat requis) ----------

    @GetMapping("/offres/{id}/postuler")
    public String formulaire(@PathVariable Integer id, HttpServletRequest request,
            Model model, RedirectAttributes redirectAttributes) {
        Optional<Integer> candidatId = candidatSession.id(request);
        if (candidatId.isEmpty()) {
            return versInscription(id);
        }
        try {
            OffreEmploi offre = offreService.trouverPubliee(id);
            if (candidatureService.aDejaPostule(id, candidatId.get())) {
                return redirigerAvecErreur(CandidatSession.PORTAIL + "/offres/" + id,
                        "Vous avez déjà postulé à cette offre.", redirectAttributes);
            }
            preparerFormulaire(model, offre);
            return "offre/candidat/candidature";
        } catch (RegleGestionException e) {
            return redirigerAvecErreur(CandidatSession.PORTAIL, e.getMessage(), redirectAttributes);
        }
    }

    @PostMapping("/offres/{id}/postuler")
    public String postuler(@PathVariable Integer id,
            @RequestParam(value = "anneeExperience", required = false) Integer anneeExperience,
            @RequestParam(value = "dernierDiplome", required = false) String dernierDiplome,
            @RequestParam(value = "pretentionSalariale", required = false) BigDecimal pretentionSalariale,
            HttpServletRequest request, Model model, RedirectAttributes redirectAttributes) {
        Optional<Integer> candidatId = candidatSession.id(request);
        if (candidatId.isEmpty()) {
            return versInscription(id);
        }

        try {
            candidatureService.postuler(id, candidatId.get(), anneeExperience, dernierDiplome, pretentionSalariale);
        } catch (RegleGestionException e) {
            // Réaffiche le formulaire avec les valeurs saisies et le message d'erreur.
            try {
                preparerFormulaire(model, offreService.trouverPubliee(id));
            } catch (RegleGestionException indisponible) {
                return redirigerAvecErreur(CandidatSession.PORTAIL, indisponible.getMessage(), redirectAttributes);
            }
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("anneeExperience", anneeExperience);
            model.addAttribute("dernierDiplome", dernierDiplome);
            model.addAttribute("pretentionSalariale", pretentionSalariale);
            return "offre/candidat/candidature";
        }

        redirectAttributes.addFlashAttribute("message", "Votre candidature a bien été envoyée. Nous reviendrons vers vous.");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:" + CandidatSession.PORTAIL + "/offres/" + id;
    }

    // ---------- Helpers ----------

    private void preparerFormulaire(Model model, OffreEmploi offre) {
        model.addAttribute("offre", offre);
        model.addAttribute("diplomes", CandidatureService.DIPLOMES);
    }

    /** Visiteur sans compte : inscription, puis retour automatique au formulaire de candidature. */
    private String versInscription(Integer offreId) {
        return "redirect:" + CandidatSession.PORTAIL + "/inscription?retour="
                + CandidatSession.PORTAIL + "/offres/" + offreId + "/postuler";
    }

    private String redirigerAvecErreur(String cible, String message, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message", message);
        redirectAttributes.addFlashAttribute("messageType", "error");
        return "redirect:" + cible;
    }
}