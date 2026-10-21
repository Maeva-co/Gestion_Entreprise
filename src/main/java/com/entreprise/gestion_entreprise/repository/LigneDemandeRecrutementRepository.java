package com.entreprise.gestion_entreprise.repository;

import com.entreprise.gestion_entreprise.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LigneDemandeRecrutementRepository extends JpaRepository<LigneDemandeRecrutement, Integer> {

    List<LigneDemandeRecrutement> findByDemandeRecrutementId(Integer demandeId);

    List<LigneDemandeRecrutement> findByStatutLigneCode(String code);

    List<LigneDemandeRecrutement> findByPosteId(Integer posteId);
}
