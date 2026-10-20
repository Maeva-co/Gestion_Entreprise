package com.entreprise.gestion_entreprise.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "historique_validation_ligne")
public class HistoriqueValidationLigne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_demande_id")
    private LigneDemandeRecrutement ligneDemande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valideur_id")
    private Utilisateur valideur;

    /** RH, FINANCE, DG */
    @Column(nullable = false, length = 50)
    private String etape;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statut_validation_id")
    private StatutValidation statutValidation;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "date_action")
    private LocalDateTime dateAction;

    @PrePersist
    void prePersist() {
        if (dateAction == null) {
            dateAction = LocalDateTime.now();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LigneDemandeRecrutement getLigneDemande() {
        return ligneDemande;
    }

    public void setLigneDemande(LigneDemandeRecrutement ligneDemande) {
        this.ligneDemande = ligneDemande;
    }

    public Utilisateur getValideur() {
        return valideur;
    }

    public void setValideur(Utilisateur valideur) {
        this.valideur = valideur;
    }

    public String getEtape() {
        return etape;
    }

    public void setEtape(String etape) {
        this.etape = etape;
    }

    public StatutValidation getStatutValidation() {
        return statutValidation;
    }

    public void setStatutValidation(StatutValidation statutValidation) {
        this.statutValidation = statutValidation;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public LocalDateTime getDateAction() {
        return dateAction;
    }

    public void setDateAction(LocalDateTime dateAction) {
        this.dateAction = dateAction;
    }

}
