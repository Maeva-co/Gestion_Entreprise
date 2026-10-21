package com.entreprise.gestion_entreprise.dto;

/**
 * Formulaire de connexion : email et mot de passe.
 * Classe régulière (et non record) pour le binding Thymeleaf via th:object.
 */
public class ConnexionRequest {

    private String email;
    private String motDePasse;

    public ConnexionRequest() {
    }

    public ConnexionRequest(String email, String motDePasse) {
        this.email = email;
        this.motDePasse = motDePasse;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }
}
