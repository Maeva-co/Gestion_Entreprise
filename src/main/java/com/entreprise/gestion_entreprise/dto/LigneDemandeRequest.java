package com.entreprise.gestion_entreprise.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Une ligne du formulaire.
 * <ul>
 *   <li>REMPLACEMENT : posteId (poste vacant), effectif, motif, dateEssaiSouhaitee, salairePropose (optionnel)</li>
 *   <li>AUGMENTATION_EFFECTIF : posteMereId, motif, salairePropose (optionnel) ; le poste fille est calculé côté serveur</li>
 * </ul>
 * Si salairePropose est vide, le budget est repris de la grille salariale.
 */
public record LigneDemandeRequest(
        Integer posteId,
        Integer posteMereId,
        Integer effectif,
        @NotBlank(message = "Le motif est obligatoire") String motif,
        @FutureOrPresent(message = "La date d'essai ne peut pas être dans le passé") LocalDate dateEssaiSouhaitee,
        BigDecimal salairePropose) {
}
