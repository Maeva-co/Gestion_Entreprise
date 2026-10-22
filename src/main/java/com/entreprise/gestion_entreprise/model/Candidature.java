package com.entreprise.gestion_entreprise.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "candidature")
public class Candidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offre_emploi_id", nullable = false)
    private OffreEmploi offreEmploi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidat_id", nullable = false)
    private Candidat candidat;

    @Column(name = "date_candidature", nullable = false, updatable = false)
    private LocalDateTime dateCandidature;

    @Column(name = "annee_experience", nullable = false)
    private Integer anneeExperience;

    @Column(name = "dernier_diplome", nullable = false, length = 150)
    private String dernierDiplome;

    @Column(name = "pretention_salariale", precision = 12, scale = 2)
    private BigDecimal pretentionSalariale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "statut_candidature_id", nullable = false)
    private StatutCandidature statutCandidature;

    @PrePersist
    void avantInsertion() {
        if (dateCandidature == null) {
            dateCandidature = LocalDateTime.now();
        }
    }

    public Integer getId() {
        return id;
    }

    public OffreEmploi getOffreEmploi() {
        return offreEmploi;
    }

    public Candidat getCandidat() {
        return candidat;
    }

    public LocalDateTime getDateCandidature() {
        return dateCandidature;
    }

    public Integer getAnneeExperience() {
        return anneeExperience;
    }

    public String getDernierDiplome() {
        return dernierDiplome;
    }

    public BigDecimal getPretentionSalariale() {
        return pretentionSalariale;
    }

    public StatutCandidature getStatutCandidature() {
        return statutCandidature;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setOffreEmploi(OffreEmploi offreEmploi) {
        this.offreEmploi = offreEmploi;
    }

    public void setCandidat(Candidat candidat) {
        this.candidat = candidat;
    }

    public void setDateCandidature(LocalDateTime dateCandidature) {
        this.dateCandidature = dateCandidature;
    }

    public void setAnneeExperience(Integer anneeExperience) {
        this.anneeExperience = anneeExperience;
    }

    public void setDernierDiplome(String dernierDiplome) {
        this.dernierDiplome = dernierDiplome;
    }

    public void setPretentionSalariale(BigDecimal pretentionSalariale) {
        this.pretentionSalariale = pretentionSalariale;
    }

    public void setStatutCandidature(StatutCandidature statutCandidature) {
        this.statutCandidature = statutCandidature;
    }
}