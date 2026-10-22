package com.entreprise.gestion_entreprise.repository;

import com.entreprise.gestion_entreprise.model.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LigneDemandeRecrutementRepository extends JpaRepository<LigneDemandeRecrutement, Integer> {

        List<LigneDemandeRecrutement> findByDemandeRecrutementId(Integer demandeId);

        List<LigneDemandeRecrutement> findByStatutLigneCode(String code);

        List<LigneDemandeRecrutement> findByPosteId(Integer posteId);

        /** Lignes EN_ATTENTE sans aucune décision RH. */
        @Query("SELECT DISTINCT l FROM LigneDemandeRecrutement l " +
                        "JOIN FETCH l.demandeRecrutement d " +
                        "JOIN FETCH d.departement " +
                        "JOIN FETCH d.demandeur " +
                        "JOIN FETCH l.poste " +
                        "JOIN FETCH l.statutLigne s " +
                        "WHERE s.code = 'EN_ATTENTE' " +
                        "AND EXISTS (" +
                        "  SELECT h FROM HistoriqueValidationLigne h " +
                        "  WHERE h.ligneDemande.id = l.id AND h.etape = 'RH' AND h.statutValidation.code = 'EN_ATTENTE'"
                        +
                        ") " +
                        "AND NOT EXISTS (" +
                        "  SELECT h FROM HistoriqueValidationLigne h " +
                        "  WHERE h.ligneDemande.id = l.id AND h.etape = 'RH' AND h.statutValidation.code IN ('APPROUVEE','REFUSEE')"
                        +
                        ")")
        List<LigneDemandeRecrutement> findLignesEnAttenteValidationRH();

        /** Lignes refusées à l'étape RH. */
        @Query("""
                        SELECT DISTINCT l FROM LigneDemandeRecrutement l
                        JOIN FETCH l.demandeRecrutement d
                        JOIN FETCH d.departement
                        JOIN FETCH d.demandeur
                        JOIN FETCH l.poste
                        JOIN FETCH l.statutLigne s
                        LEFT JOIN FETCH l.historiques hist
                        LEFT JOIN FETCH hist.statutValidation
                        WHERE s.code = 'REFUSEE'
                          AND EXISTS (
                              SELECT h FROM HistoriqueValidationLigne h
                              WHERE h.ligneDemande = l AND h.etape = 'RH'
                                AND h.statutValidation.code = 'REFUSEE')
                        """)
        List<LigneDemandeRecrutement> findLignesRefuseesParRH();

        

        /** Lignes approuvées par le RH, sans décision DG. */
        @Query("""
                        SELECT l FROM LigneDemandeRecrutement l
                        WHERE EXISTS (
                              SELECT h FROM HistoriqueValidationLigne h
                              WHERE h.ligneDemande = l AND h.etape = 'RH'
                                AND h.statutValidation.code = 'APPROUVEE')
                          AND NOT EXISTS (
                              SELECT h2 FROM HistoriqueValidationLigne h2
                              WHERE h2.ligneDemande = l AND h2.etape = 'DG'
                                AND h2.statutValidation.code IN ('APPROUVEE', 'REFUSEE'))
                        """)
        List<LigneDemandeRecrutement> findLignesApprouveesParRHEnAttenteDG();

        /** Charge la ligne avec un verrou pessimiste (décisions concurrentes). */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT l FROM LigneDemandeRecrutement l WHERE l.id = :id")
        Optional<LigneDemandeRecrutement> findByIdForUpdate(@Param("id") Integer id);
}