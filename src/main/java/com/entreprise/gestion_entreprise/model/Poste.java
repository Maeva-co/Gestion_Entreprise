package com.entreprise.gestion_entreprise.model;

import jakarta.persistence.*;

@Entity
@Table(name = "poste")
public class Poste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departement_id")
    private Departement departement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grille_salariale_id")
    private GrilleSalariale grilleSalariale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statut_poste_id")
    private StatutPoste statutPoste;

    /** Poste mère (héritage mère-fille), nullable. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_parent_id")
    private Poste posteParent;

    public Integer getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public String getDescription() {
        return description;
    }

    public Departement getDepartement() {
        return departement;
    }

    public GrilleSalariale getGrilleSalariale() {
        return grilleSalariale;
    }

    public StatutPoste getStatutPoste() {
        return statutPoste;
    }

    public Poste getPosteParent() {
        return posteParent;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDepartement(Departement departement) {
        this.departement = departement;
    }

    public void setGrilleSalariale(GrilleSalariale grilleSalariale) {
        this.grilleSalariale = grilleSalariale;
    }

    public void setStatutPoste(StatutPoste statutPoste) {
        this.statutPoste = statutPoste;
    }

    public void setPosteParent(Poste posteParent) {
        this.posteParent = posteParent;
    }

}
