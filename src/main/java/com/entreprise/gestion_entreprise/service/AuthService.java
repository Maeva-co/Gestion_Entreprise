package com.entreprise.gestion_entreprise.service;

import com.entreprise.gestion_entreprise.exception.RegleGestionException;
import com.entreprise.gestion_entreprise.model.Utilisateur;
import com.entreprise.gestion_entreprise.repository.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service d'authentification : valide les identifiants (email / mot de passe)
 * et retourne l'utilisateur connecté. Les mots de passe sont stockés hashés (BCrypt).
 */
@Service
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UtilisateurRepository utilisateurRepository,
                       PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authentifie un utilisateur par son email et son mot de passe.
     *
     * @param email      email de l'utilisateur
     * @param motDePasse mot de passe en clair saisi (comparé au hash BCrypt stocké)
     * @return l'utilisateur authentifié
     * @throws RegleGestionException si l'email est inconnu ou le mot de passe est erroné
     */
    @Transactional(readOnly = true)
    public Utilisateur authentifier(String email, String motDePasse) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RegleGestionException("Identifiants invalides"));

        if (!passwordEncoder.matches(motDePasse, utilisateur.getMotDePasse())) {
            throw new RegleGestionException("Identifiants invalides");
        }

        return utilisateur;
    }
    
}