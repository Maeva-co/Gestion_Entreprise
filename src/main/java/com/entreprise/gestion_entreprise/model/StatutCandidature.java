package com.entreprise.gestion_entreprise.model;

import jakarta.persistence.*;

@Entity
@Table(name = "statut_candidature")
public class StatutCandidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String libelle;

    /** Position dans le pipeline (1 = RECUE, ...). */
    @Column(nullable = false)
    private Integer ordre;

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getLibelle() {
        return libelle;
    }

    public Integer getOrdre() {
        return ordre;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public void setOrdre(Integer ordre) {
        this.ordre = ordre;
    }
}