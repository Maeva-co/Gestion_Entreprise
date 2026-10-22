package com.entreprise.gestion_entreprise.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "offre_emploi")
public class OffreEmploi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Ligne de demande (validée par le DG) dont l'offre est issue. Une ligne = une offre. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ligne_demande_id", nullable = false, unique = true)
    private LigneDemandeRecrutement ligneDemande;

    @Column(name = "titre_offre", nullable = false, length = 200)
    private String titreOffre;

    @Column(name = "description_offre", columnDefinition = "TEXT")
    private String descriptionOffre;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_publication")
    private LocalDateTime datePublication;

    @Column(name = "date_cloture")
    private LocalDateTime dateCloture;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "statut_offre_id", nullable = false)
    private StatutOffre statutOffre;

    @OneToMany(mappedBy = "offreEmploi", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CritereOffre> criteres = new ArrayList<>();

    @PrePersist
    void avantInsertion() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }

    public void ajouterCritere(CritereOffre critere) {
        criteres.add(critere);
        critere.setOffreEmploi(this);
    }

    public void retirerCritere(CritereOffre critere) {
        criteres.remove(critere);
        critere.setOffreEmploi(null);
    }

    public Integer getId() {
        return id;
    }

    public LigneDemandeRecrutement getLigneDemande() {
        return ligneDemande;
    }

    public String getTitreOffre() {
        return titreOffre;
    }

    public String getDescriptionOffre() {
        return descriptionOffre;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public LocalDateTime getDatePublication() {
        return datePublication;
    }

    public LocalDateTime getDateCloture() {
        return dateCloture;
    }

    public StatutOffre getStatutOffre() {
        return statutOffre;
    }

    public List<CritereOffre> getCriteres() {
        return criteres;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setLigneDemande(LigneDemandeRecrutement ligneDemande) {
        this.ligneDemande = ligneDemande;
    }

    public void setTitreOffre(String titreOffre) {
        this.titreOffre = titreOffre;
    }

    public void setDescriptionOffre(String descriptionOffre) {
        this.descriptionOffre = descriptionOffre;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public void setDatePublication(LocalDateTime datePublication) {
        this.datePublication = datePublication;
    }

    public void setDateCloture(LocalDateTime dateCloture) {
        this.dateCloture = dateCloture;
    }

    public void setStatutOffre(StatutOffre statutOffre) {
        this.statutOffre = statutOffre;
    }

    public void setCriteres(List<CritereOffre> criteres) {
        this.criteres = criteres;
    }
}