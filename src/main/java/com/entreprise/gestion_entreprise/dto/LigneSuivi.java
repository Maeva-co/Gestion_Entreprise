package com.entreprise.gestion_entreprise.dto;

import com.entreprise.gestion_entreprise.model.LigneDemandeRecrutement;

/** Une ligne de recrutement et son étape d'avancement, pour la page de suivi. */
public class LigneSuivi {

    private final LigneDemandeRecrutement ligne;
    private final StatutSuivi statut;

    public LigneSuivi(LigneDemandeRecrutement ligne, StatutSuivi statut) {
        this.ligne = ligne;
        this.statut = statut;
    }

    public LigneDemandeRecrutement getLigne() {
        return ligne;
    }

    public StatutSuivi getStatut() {
        return statut;
    }
}