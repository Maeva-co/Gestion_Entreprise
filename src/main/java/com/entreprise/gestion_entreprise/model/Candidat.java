package com.entreprise.gestion_entreprise.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "candidat")
public class Candidat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /** Hash BCrypt du mot de passe (jamais le mot de passe en clair). */
    @Column(name = "mot_de_passe", nullable = false, length = 100)
    private String motDePasse;

    @Column(length = 30)
    private String telephone;

    /** Chemin du CV (PDF/Docx) stocké sur le serveur. */
    @Column(name = "chemin_cv", length = 255)
    private String cheminCv;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    void avantInsertion() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }

    public Integer getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getEmail() {
        return email;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public String getTelephone() {
        return telephone;
    }

    public String getCheminCv() {
        return cheminCv;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public void setCheminCv(String cheminCv) {
        this.cheminCv = cheminCv;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
}