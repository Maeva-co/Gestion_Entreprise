package com.entreprise.gestion_entreprise.repository;

import com.entreprise.gestion_entreprise.model.TypeDemandeRecrutement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TypeDemandeRecrutementRepository extends JpaRepository<TypeDemandeRecrutement, Integer> {
}