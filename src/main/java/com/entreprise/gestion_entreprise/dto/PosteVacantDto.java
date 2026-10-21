package com.entreprise.gestion_entreprise.dto;

import java.math.BigDecimal;

/** Poste vacant proposé dans le cas 1, avec le budget suggéré par la grille salariale. */
public record PosteVacantDto(
        Integer id,
        String titre,
        Integer grilleSalarialeId,
        String niveau,
        BigDecimal salaireSuggere) {
}
