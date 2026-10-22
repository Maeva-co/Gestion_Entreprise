package com.entreprise.gestion_entreprise.controller;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.entreprise.gestion_entreprise.config.CircuitValidation;
import com.entreprise.gestion_entreprise.config.EtapeValidation;
import com.entreprise.gestion_entreprise.dto.LigneSuivi;
import com.entreprise.gestion_entreprise.dto.StatutSuivi;
import com.entreprise.gestion_entreprise.service.ValidationLigneService;

/**
 * Suivi des demandes de recrutement (département, manager, avancement).
 * Page en lecture seule, indépendante du rôle : l'accès se règle dans la SecurityConfig.
 * Les onglets sont construits à partir des étapes de CircuitValidation.
 */
@Controller
@RequestMapping("/demandes-recrutement/suivi")
public class SuiviDemandeController {

    private final ValidationLigneService validationService;

    /** Onglets de la page : code du filtre -> libellé, dans l'ordre d'affichage. */
    private final Map<String, String> filtres;

    public SuiviDemandeController(ValidationLigneService validationService, CircuitValidation circuit) {
        this.validationService = validationService;

        Map<String, String> onglets = new LinkedHashMap<>();
        for (EtapeValidation etape : circuit.getEtapes()) {
            onglets.put(StatutSuivi.filtreAValider(etape), "À valider " + etape.getLibelle());
        }
        onglets.put(StatutSuivi.FILTRE_VALIDEE, "Validées");
        onglets.put(StatutSuivi.FILTRE_REFUSEE, "Refusées");
        this.filtres = Collections.unmodifiableMap(onglets);
    }

    @GetMapping
    public String liste(@RequestParam(value = "statut", required = false) String statut,
            Model model) {
        List<LigneSuivi> toutes = validationService.getSuiviLignes();

        // Filtre accepté seulement s'il correspond à un onglet connu.
        String statutActif = (statut != null && filtres.containsKey(statut)) ? statut : null;

        List<LigneSuivi> lignes = (statutActif == null)
                ? toutes
                : toutes.stream()
                        .filter(l -> l.getStatut().getFiltre().equals(statutActif))
                        .toList();

        model.addAttribute("lignes", lignes);
        model.addAttribute("statutActif", statutActif);
        model.addAttribute("filtres", filtres);
        model.addAttribute("compteurs", compter(toutes));
        model.addAttribute("total", toutes.size());
        return "demande-recrutement/suivi-liste";
    }

    /** Nombre de lignes par onglet. Tous les onglets sont présents, même à 0. */
    private Map<String, Long> compter(List<LigneSuivi> lignes) {
        Map<String, Long> compteurs = new LinkedHashMap<>();
        for (String code : filtres.keySet()) {
            compteurs.put(code, 0L);
        }
        for (LigneSuivi l : lignes) {
            compteurs.merge(l.getStatut().getFiltre(), 1L, Long::sum);
        }
        return compteurs;
    }
}