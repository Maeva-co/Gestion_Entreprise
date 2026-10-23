package com.entreprise.gestion_entreprise.portail;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.entreprise.gestion_entreprise.model.Candidat;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Session du candidat connecté au portail public. Indépendante de la connexion du
 * personnel (RH, DG, Finance) : seul l'identifiant du candidat est gardé en session.
 */
@Component
public class CandidatSession {

    public static final String PORTAIL = "/candidate-portal";
    private static final String CLE = "candidatId";

    /** Identifiant du candidat connecté, s'il y en a un (ne crée pas de session). */
    public Optional<Integer> id(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }
        Object valeur = session.getAttribute(CLE);
        return (valeur instanceof Integer id) ? Optional.of(id) : Optional.empty();
    }

    /** Connecte le candidat ; l'identifiant de session est renouvelé (anti fixation de session). */
    public void ouvrir(HttpServletRequest request, Candidat candidat) {
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        request.getSession(true).setAttribute(CLE, candidat.getId());
    }

    /** Déconnecte le candidat sans toucher à une éventuelle connexion du personnel. */
    public void fermer(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(CLE);
        }
    }

    /** Adresse de retour acceptée seulement si elle reste dans le portail (pas de redirection externe). */
    public String retourSur(String retour) {
        if (retour != null && retour.matches("/candidate-portal[A-Za-z0-9/_.~%?=&-]*")) {
            return retour;
        }
        return PORTAIL;
    }
}