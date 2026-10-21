package com.entreprise.gestion_entreprise.model;


/** Valeurs stockées dans ligne_demande_recrutement.type_besoin. */

public enum TypeBesoin {
    REMPLACEMENT("Remplacement (poste vacant)"),
    AUGMENTATION_EFFECTIF("Augmentation d'effectif (poste fille)");

    private final String libelle;

    public String getLibelle() {
        return libelle;
    }

    private TypeBesoin(String libelle) {
        this.libelle = libelle;
    }


}
