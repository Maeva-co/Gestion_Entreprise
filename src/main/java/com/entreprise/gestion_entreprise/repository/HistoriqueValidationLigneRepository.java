package com.entreprise.gestion_entreprise.repository;

import com.entreprise.gestion_entreprise.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoriqueValidationLigneRepository extends JpaRepository<HistoriqueValidationLigne, Integer> {

    List<HistoriqueValidationLigne> findByLigneDemandeIdOrderByDateActionAsc(Integer ligneId);

    List<HistoriqueValidationLigne> findByLigneDemandeIdAndEtape(Integer ligneId, String etape);

    List<HistoriqueValidationLigne> findByValideurId(Integer valideurId);
}
