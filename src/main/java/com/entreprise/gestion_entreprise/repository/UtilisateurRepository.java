package com.entreprise.gestion_entreprise.repository;

import com.entreprise.gestion_entreprise.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {

    @Query("SELECT u FROM Utilisateur u JOIN FETCH u.role WHERE u.email = :email")
    Optional<Utilisateur> findByEmail(@Param("email") String email);

    boolean existsByEmail(String email);

    List<Utilisateur> findByDepartementId(Integer departementId);

    List<Utilisateur> findByRoleCode(String roleCode);
}
