package com.entreprise.gestion_entreprise.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.entreprise.gestion_entreprise.model.Candidature;

public interface CandidatureRepository extends JpaRepository<Candidature, Integer> {

    /** Candidatures d'une offre, de la plus récente à la plus ancienne. */
    @Query("""
            SELECT c FROM Candidature c
            JOIN FETCH c.candidat
            JOIN FETCH c.statutCandidature
            WHERE c.offreEmploi.id = :offreId
            ORDER BY c.dateCandidature DESC
            """)
    List<Candidature> findByOffreAvecDetails(@Param("offreId") Integer offreId);

    /** Même liste, filtrée sur un statut du pipeline. */
    @Query("""
            SELECT c FROM Candidature c
            JOIN FETCH c.candidat
            JOIN FETCH c.statutCandidature s
            WHERE c.offreEmploi.id = :offreId AND s.code = :codeStatut
            ORDER BY c.dateCandidature DESC
            """)
    List<Candidature> findByOffreEtStatutAvecDetails(@Param("offreId") Integer offreId,
            @Param("codeStatut") String codeStatut);

    /** Lignes {code du statut, nombre de candidatures} pour une offre. */
    @Query("""
            SELECT s.code, COUNT(c)
            FROM Candidature c JOIN c.statutCandidature s
            WHERE c.offreEmploi.id = :offreId
            GROUP BY s.code
            """)
    List<Object[]> compterParStatut(@Param("offreId") Integer offreId);

    long countByOffreEmploiId(Integer offreId);

    boolean existsByOffreEmploiIdAndCandidatId(Integer offreId, Integer candidatId);
}