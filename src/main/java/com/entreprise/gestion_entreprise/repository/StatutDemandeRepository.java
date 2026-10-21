package com.entreprise.gestion_entreprise.repository;

import com.entreprise.gestion_entreprise.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatutDemandeRepository extends JpaRepository<StatutDemande, Integer> {

    Optional<StatutDemande> findByCode(String code);
}
