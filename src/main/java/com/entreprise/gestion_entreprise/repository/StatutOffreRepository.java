package com.entreprise.gestion_entreprise.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.entreprise.gestion_entreprise.model.StatutOffre;

@Repository
public interface StatutOffreRepository extends JpaRepository<StatutOffre, Integer> {

    Optional<StatutOffre> findByCode(String code);
}