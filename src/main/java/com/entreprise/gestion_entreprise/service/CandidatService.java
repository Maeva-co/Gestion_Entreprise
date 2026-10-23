package com.entreprise.gestion_entreprise.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.entreprise.gestion_entreprise.exception.RegleGestionException;
import com.entreprise.gestion_entreprise.model.Candidat;
import com.entreprise.gestion_entreprise.repository.CandidatRepository;

/**
 * Comptes candidats du portail public : inscription (au moment de postuler) et connexion.
 * Les mots de passe sont stockés hachés (BCrypt). L'encodeur est créé ici et non déclaré
 * comme bean pour ne pas interférer avec la configuration de sécurité du personnel.
 */
@Service
public class CandidatService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MOT_DE_PASSE_MIN = 8;
    /** Limite de BCrypt : au-delà de 72 octets le mot de passe serait tronqué. */
    private static final int MOT_DE_PASSE_MAX_OCTETS = 72;

    private final CandidatRepository candidatRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    /** Sert à dépenser le même temps de calcul quand l'email est inconnu. */
    private final String hashFactice = passwordEncoder.encode("hash-factice");

    public CandidatService(CandidatRepository candidatRepository) {
        this.candidatRepository = candidatRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Candidat> trouver(Integer id) {
        return candidatRepository.findById(id);
    }
 
    /** Crée un compte candidat : nom, prénom, email et mot de passe uniquement. */
    @Transactional
    public Candidat inscrire(String nom, String prenom, String email, String motDePasse, String confirmation) {
        String nomNet = nettoyer(nom);
        String prenomNet = nettoyer(prenom);
        String emailNet = nettoyer(email).toLowerCase(Locale.ROOT);
 
        if (nomNet.isEmpty() || prenomNet.isEmpty() || emailNet.isEmpty()) {
            throw new RegleGestionException("Le nom, le prénom et l'email sont obligatoires.");
        }
        if (nomNet.length() > 100 || prenomNet.length() > 100 || emailNet.length() > 150) {
            throw new RegleGestionException("Un des champs saisis est trop long.");
        }
        if (!EMAIL.matcher(emailNet).matches()) {
            throw new RegleGestionException("L'adresse email n'est pas valide.");
        }
        if (motDePasse == null || motDePasse.length() < MOT_DE_PASSE_MIN) {
            throw new RegleGestionException(
                    "Le mot de passe doit contenir au moins " + MOT_DE_PASSE_MIN + " caractères.");
        }
        if (motDePasse.getBytes(StandardCharsets.UTF_8).length > MOT_DE_PASSE_MAX_OCTETS) {
            throw new RegleGestionException("Le mot de passe est trop long.");
        }
        if (!motDePasse.equals(confirmation)) {
            throw new RegleGestionException("Les deux mots de passe ne correspondent pas.");
        }
        if (candidatRepository.findByEmail(emailNet).isPresent()) {
            throw new RegleGestionException("Un compte existe déjà avec cet email : connectez-vous.");
        }
 
        Candidat candidat = new Candidat();
        candidat.setNom(nomNet);
        candidat.setPrenom(prenomNet);
        candidat.setEmail(emailNet);
        candidat.setMotDePasse(passwordEncoder.encode(motDePasse));
        try {
            return candidatRepository.saveAndFlush(candidat);
        } catch (DataIntegrityViolationException e) {
            // Deux inscriptions simultanées avec le même email.
            throw new RegleGestionException("Un compte existe déjà avec cet email : connectez-vous.");
        }
    }
 
    /** Vérifie l'email et le mot de passe ; même message d'erreur dans tous les cas d'échec. */
    @Transactional(readOnly = true)
    public Candidat authentifier(String email, String motDePasse) {
        String emailNet = nettoyer(email).toLowerCase(Locale.ROOT);
        String saisi = motDePasse == null ? "" : motDePasse;
 
        Candidat candidat = candidatRepository.findByEmail(emailNet).orElse(null);
        String hash = (candidat != null) ? candidat.getMotDePasse() : null;
 
        boolean valide = false;
        if (hash != null) {
            valide = passwordEncoder.matches(saisi, hash);
        } else {
            // Email inconnu : même temps de calcul, résultat toujours négatif.
            passwordEncoder.matches(saisi, hashFactice);
        }
        if (!valide) {
            throw new RegleGestionException("Email ou mot de passe incorrect.");
        }
        return candidat;
    }
 
    private String nettoyer(String valeur) {
        return valeur == null ? "" : valeur.trim();
    }
}