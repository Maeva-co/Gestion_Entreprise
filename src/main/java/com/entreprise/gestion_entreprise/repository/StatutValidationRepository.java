package com.entreprise.gestion_entreprise.repository;

import com.entreprise.gestion_entreprise.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatutValidationRepository extends JpaRepository<StatutValidation, Integer> {

    Optional<StatutValidation> findByCode(String code);
}
