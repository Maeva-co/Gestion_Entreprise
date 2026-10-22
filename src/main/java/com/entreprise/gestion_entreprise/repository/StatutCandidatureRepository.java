package com.entreprise.gestion_entreprise.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.entreprise.gestion_entreprise.model.StatutCandidature;

@Repository
public interface StatutCandidatureRepository extends JpaRepository<StatutCandidature, Integer> {

    Optional<StatutCandidature> findByCode(String code);

    List<StatutCandidature> findAllByOrderByOrdreAsc();
}