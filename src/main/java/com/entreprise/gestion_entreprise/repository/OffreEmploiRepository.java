package com.entreprise.gestion_entreprise.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.entreprise.gestion_entreprise.model.OffreEmploi;

@Repository
public interface OffreEmploiRepository extends JpaRepository<OffreEmploi, Integer> {

        Optional<OffreEmploi> findByLigneDemandeId(Integer ligneId);

        boolean existsByLigneDemandeId(Integer ligneId);

        List<OffreEmploi> findByStatutOffreCodeOrderByDateCreationDesc(String code);

        long countByStatutOffreCode(String code);

        /**
         * Liste complète pour l'espace RH. Ligne, poste, département et statut sont
         * chargés en une seule requête (évite le problème N+1 dans la vue).
         */
        @Query("select o from OffreEmploi o "
                        + "join fetch o.ligneDemande l "
                        + "join fetch l.poste "
                        + "join fetch l.demandeRecrutement d "
                        + "join fetch d.departement "
                        + "join fetch o.statutOffre "
                        + "order by o.dateCreation desc")
        List<OffreEmploi> findAllAvecDetails();

        /** Même liste, filtrée sur le code du statut (BROUILLON, PUBLIEE, ...). */
        @Query("select o from OffreEmploi o "
                        + "join fetch o.ligneDemande l "
                        + "join fetch l.poste "
                        + "join fetch l.demandeRecrutement d "
                        + "join fetch d.departement "
                        + "join fetch o.statutOffre s "
                        + "where s.code = :code "
                        + "order by o.dateCreation desc")
        List<OffreEmploi> findByStatutAvecDetails(@Param("code") String code);

        /** Nombre d'offres par code de statut : chaque élément est [code, total]. */
        @Query("select o.statutOffre.code, count(o) from OffreEmploi o group by o.statutOffre.code")
        List<Object[]> compterParStatut();

        @Query("SELECT o FROM OffreEmploi o JOIN FETCH o.statutOffre WHERE o.id = :id")
        Optional<OffreEmploi> findByIdAvecStatut(@Param("id") Integer id);
}