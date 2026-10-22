package com.entreprise.gestion_entreprise.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.entreprise.gestion_entreprise.model.CritereOffre;

@Repository
public interface CritereOffreRepository extends JpaRepository<CritereOffre, Integer> {

    List<CritereOffre> findByOffreEmploiIdOrderByIdAsc(Integer offreId);
}