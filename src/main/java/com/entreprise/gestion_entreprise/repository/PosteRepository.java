package com.entreprise.gestion_entreprise.repository;

import com.entreprise.gestion_entreprise.model.Poste;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PosteRepository extends JpaRepository<Poste, Integer> {

    List<Poste> findByDepartementId(Integer departementId);

    List<Poste> findByStatutPosteCode(String code);

    /** Postes filles d'un poste mère (la grille est chargée avec pour éviter le N+1). */
    @EntityGraph(attributePaths = {"grilleSalariale"})
    List<Poste> findByPosteParentId(Integer posteParentId);

    /** Postes d'un département dans un statut donné (ex. VACANT). */
    @EntityGraph(attributePaths = {"grilleSalariale"})
    List<Poste> findByDepartementIdAndStatutPosteCodeOrderByTitreAsc(Integer departementId, String code);

    /** Postes mères (sans parent) d'un département. */
    List<Poste> findByDepartementIdAndPosteParentIsNullOrderByTitreAsc(Integer departementId);
}
