package com.entreprise.gestion_entreprise.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.entreprise.gestion_entreprise.model.Candidat;

public interface CandidatRepository extends JpaRepository<Candidat, Integer> {

    Optional<Candidat> findByEmail(String email);
}