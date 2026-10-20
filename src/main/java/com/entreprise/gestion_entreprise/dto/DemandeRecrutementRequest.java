package com.entreprise.gestion_entreprise.dto;

import com.entreprise.gestion_entreprise.model.TypeBesoin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DemandeRecrutementRequest(
        @NotNull(message = "Le département demandeur est obligatoire") Integer departementId,
        @NotNull(message = "Le type de demande est obligatoire") TypeBesoin typeBesoin,
        @NotEmpty(message = "Ajoutez au moins une ligne") List<@Valid LigneDemandeRequest> lignes) {
}
