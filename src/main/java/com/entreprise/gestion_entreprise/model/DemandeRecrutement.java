package com.entreprise.gestion_entreprise.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "demande_recrutement")
public class DemandeRecrutement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demandeur_id")
    private Utilisateur demandeur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departement_id")
    private Departement departement;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statut_demande_id")
    private StatutDemande statutDemande;

    @OneToMany(mappedBy = "demandeRecrutement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneDemandeRecrutement> lignes = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }


    public void addLigne(LigneDemandeRecrutement ligne) {
        lignes.add(ligne);
        ligne.setDemandeRecrutement(this);
    }

    public void removeLigne(LigneDemandeRecrutement ligne) {
        lignes.remove(ligne);
        ligne.setDemandeRecrutement(null);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Utilisateur getDemandeur() {
        return demandeur;
    }

    public void setDemandeur(Utilisateur demandeur) {
        this.demandeur = demandeur;
    }

    public Departement getDepartement() {
        return departement;
    }

    public void setDepartement(Departement departement) {
        this.departement = departement;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public StatutDemande getStatutDemande() {
        return statutDemande;
    }

    public void setStatutDemande(StatutDemande statutDemande) {
        this.statutDemande = statutDemande;
    }

    public List<LigneDemandeRecrutement> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneDemandeRecrutement> lignes) {
        this.lignes = lignes;
    }
}
