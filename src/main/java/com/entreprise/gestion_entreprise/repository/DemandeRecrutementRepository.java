package com.entreprise.gestion_entreprise.repository;

import com.entreprise.gestion_entreprise.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandeRecrutementRepository extends JpaRepository<DemandeRecrutement, Integer> {

    List<DemandeRecrutement> findByDemandeurId(Integer demandeurId);

    List<DemandeRecrutement> findByDepartementId(Integer departementId);

    List<DemandeRecrutement> findByStatutDemandeCode(String code);
}
