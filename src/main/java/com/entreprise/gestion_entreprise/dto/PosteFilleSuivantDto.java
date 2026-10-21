package com.entreprise.gestion_entreprise.dto;

import java.math.BigDecimal;

/** Poste fille calculé automatiquement pour le cas 2 (augmentation d'effectif). */
public record PosteFilleSuivantDto(
        Integer posteMereId,
        String titreMere,
        int numero,
        String titrePosteFille,
        Integer grilleSalarialeId,
        String niveau,
        BigDecimal salaireSuggere) {
}
