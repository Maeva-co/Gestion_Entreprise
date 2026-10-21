package com.entreprise.gestion_entreprise.controller;

import com.entreprise.gestion_entreprise.model.DemandeRecrutement;
import com.entreprise.gestion_entreprise.model.Utilisateur;
import com.entreprise.gestion_entreprise.repository.UtilisateurRepository;
import com.entreprise.gestion_entreprise.service.DemandeRecrutementService;
import com.entreprise.gestion_entreprise.service.DemandeRecrutementService.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/demandes-recrutement")
public class DemandeRecrutementController {

    private final DemandeRecrutementService service;
    private final UtilisateurRepository utilisateurRepository;

    public DemandeRecrutementController(DemandeRecrutementService service,
                                        UtilisateurRepository utilisateurRepository) {
        this.service = service;
        this.utilisateurRepository = utilisateurRepository;
    }

    // ---------- Page du formulaire ----------

    @GetMapping("/nouvelle")
    public String afficherFormulaire(Model model) {
        model.addAttribute("departements", service.getDepartements());
        model.addAttribute("types", service.getTypesDemande());
        return "demande-recrutement/form";
    }

    /** Postes mères du département choisi (chargés par le JS au changement de département). */
    @GetMapping("/api/postes-meres")
    @ResponseBody
    public List<PosteDto> postesMeres(@RequestParam Integer departementId) {
        return service.getPostesMeres(departementId);
    }

    // ---------- Endpoints appelés par le JS ----------

    /** Type 1 : postes filles vacants du poste mère dans le département + effectif max. */
    @GetMapping("/api/postes-vacants")
    @ResponseBody
    public List<PosteVacantDto> postesVacants(@RequestParam Integer departementId,
                                              @RequestParam Integer posteMereId) {
        return service.getPostesVacants(departementId, posteMereId);
    }

    /** Type 1 : budget = grille salariale du poste mère. */
    @GetMapping("/api/budget-mere")
    @ResponseBody
    public BudgetDto budgetMere(@RequestParam Integer posteMereId) {
        return service.getBudgetPosteMere(posteMereId);
    }

    /** Type 2 : prochain poste à créer (DEV 4) + budget du poste similaire du département. */
    @GetMapping("/api/prochain-poste")
    @ResponseBody
    public ProchainPosteDto prochainPoste(@RequestParam Integer departementId,
                                          @RequestParam Integer posteMereId) {
        return service.getProchainPoste(departementId, posteMereId);
    }

    // ---------- Enregistrement ----------

    @PostMapping("/api/creer")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> creer(@RequestBody DemandeForm form, Principal principal) {
        try {
            // À adapter : récupération de l'utilisateur connecté (email, login, session...).
            // DEV : sans connexion (principal nul), on prend le premier utilisateur de la base.
            Utilisateur demandeur = (principal != null
                    ? utilisateurRepository.findByEmail(principal.getName())
                    : utilisateurRepository.findAll().stream().findFirst())
                    .orElseThrow(() -> new IllegalStateException("Aucun utilisateur trouvé : insérez des utilisateurs de test."));

            DemandeRecrutement demande = service.creer(form, demandeur);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("id", demande.getId(), "message", "Demande de recrutement créée."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    /** Les endpoints GET renvoient aussi un message lisible par le JS en cas d'erreur de validation. */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}