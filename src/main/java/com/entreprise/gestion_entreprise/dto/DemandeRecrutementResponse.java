package com.entreprise.gestion_entreprise.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DemandeRecrutementResponse(
        Integer id,
        LocalDateTime dateCreation,
        String statut,
        int nombreLignes,
        List<String> postesCrees) {
}
