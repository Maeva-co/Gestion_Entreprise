package com.entreprise.gestion_entreprise.model;

import jakarta.persistence.*;

@Entity
@Table(name = "critere_offre")
public class CritereOffre {

    public static final String TYPE_EXPERIENCE = "EXPERIENCE";
    public static final String TYPE_DIPLOME = "DIPLOME";
    public static final String TYPE_COMPETENCE = "COMPETENCE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offre_emploi_id", nullable = false)
    private OffreEmploi offreEmploi;

    /** Ex. : « Années d'expérience Java ». */
    @Column(nullable = false, length = 150)
    private String intitule;

    /** EXPERIENCE, DIPLOME ou COMPETENCE. */
    @Column(name = "type_critere", nullable = false, length = 30)
    private String typeCritere;

    /** Ex. : « 3 », « Bac+5 ». */
    @Column(name = "valeur_requise", nullable = false, length = 100)
    private String valeurRequise;

    @Column(name = "est_obligatoire", nullable = false)
    private boolean estObligatoire = true;

    public Integer getId() {
        return id;
    }

    public OffreEmploi getOffreEmploi() {
        return offreEmploi;
    }

    public String getIntitule() {
        return intitule;
    }

    public String getTypeCritere() {
        return typeCritere;
    }

    public String getValeurRequise() {
        return valeurRequise;
    }

    public boolean isEstObligatoire() {
        return estObligatoire;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setOffreEmploi(OffreEmploi offreEmploi) {
        this.offreEmploi = offreEmploi;
    }

    public void setIntitule(String intitule) {
        this.intitule = intitule;
    }

    public void setTypeCritere(String typeCritere) {
        this.typeCritere = typeCritere;
    }

    public void setValeurRequise(String valeurRequise) {
        this.valeurRequise = valeurRequise;
    }

    public void setEstObligatoire(boolean estObligatoire) {
        this.estObligatoire = estObligatoire;
    }
}