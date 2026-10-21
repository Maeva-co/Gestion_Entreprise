package com.entreprise.gestion_entreprise.repository;

import com.entreprise.gestion_entreprise.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatutPosteRepository extends JpaRepository<StatutPoste, Integer> {

    Optional<StatutPoste> findByCode(String code);
}
