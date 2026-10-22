package com.entreprise.gestion_entreprise.controller;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.entreprise.gestion_entreprise.exception.RegleGestionException;
import com.entreprise.gestion_entreprise.model.OffreEmploi;
import com.entreprise.gestion_entreprise.service.OffreEmploiService;

/**
 * Espace RH : gestion des offres d'emploi issues des demandes validées par le DG.
 * Accès réservé au rôle RH (voir SecurityConfig : /offres/**).
 * La liste publique pour les candidats sera sous /candidate-portal/...
 */
@Controller
@RequestMapping("/offres")
public class OffreController {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final OffreEmploiService offreService;

    public OffreController(OffreEmploiService offreService) {
        this.offreService = offreService;
    }

    @GetMapping
    public String liste(@RequestParam(value = "statut", required = false) String statut,
            Model model) {
        // Filtre accepté seulement s'il correspond à un statut connu.
        String statutActif = (statut != null && OffreEmploiService.CODES_STATUT.contains(statut))
                ? statut
                : null;

        List<OffreEmploi> offres = offreService.lister(statutActif);

        model.addAttribute("offres", offres);
        model.addAttribute("statutActif", statutActif);
        model.addAttribute("compteurs", offreService.compterParStatut());
        model.addAttribute("totalOffres", offreService.compterToutes());
        model.addAttribute("transitions", OffreEmploiService.TRANSITIONS);
        return "offre/rh/liste";
    }

    /**
     * Changement de statut appelé en AJAX (fetch) depuis le modal de la liste.
     * Retourne le nouvel état de l'offre et les compteurs à jour, pour mettre la
     * page à jour sans la recharger.
     */
    @PostMapping("/{id}/statut")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changerStatut(@PathVariable Integer id,
            @RequestParam("statut") String statut) {
        Map<String, Object> corps = new LinkedHashMap<>();
        try {
            OffreEmploi offre = offreService.changerStatut(id, statut);
            String code = offre.getStatutOffre().getCode();

            corps.put("statut", code);
            corps.put("libelle", offre.getStatutOffre().getLibelle());
            corps.put("datePublication",
                    offre.getDatePublication() != null ? offre.getDatePublication().format(FORMAT_DATE) : null);
            corps.put("transitions", OffreEmploiService.transitionsDepuis(code));
            corps.put("compteurs", offreService.compterParStatut());
            return ResponseEntity.ok(corps);
        } catch (RegleGestionException e) {
            corps.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(corps);
        }
    }
}