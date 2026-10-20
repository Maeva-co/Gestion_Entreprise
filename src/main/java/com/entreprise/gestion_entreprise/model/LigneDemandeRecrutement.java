package com.entreprise.gestion_entreprise.model;

import jakarta.persistence.*;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ligne_demande_recrutement")
public class LigneDemandeRecrutement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_recrutement_id")
    private DemandeRecrutement demandeRecrutement;

    /** REMPLACEMENT, AUGMENTATION_EFFECTIF, CREATION */
    @Column(name = "type_besoin", nullable = false, length = 50)
    private String typeBesoin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_id")
    private Poste poste;

    @Column(name = "nombre_postes", nullable = false)
    private Integer nombrePostes = 1;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Column(name = "date_essai_souhaitee")
    private LocalDate dateEssaiSouhaitee;

    @Column(name = "salaire_propose", nullable = false, precision = 12, scale = 2)
    private BigDecimal salairePropose;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statut_ligne_id")
    private StatutLigne statutLigne;

    @OneToMany(mappedBy = "ligneDemande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HistoriqueValidationLigne> historiques = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public DemandeRecrutement getDemandeRecrutement() {
        return demandeRecrutement;
    }

    public void setDemandeRecrutement(DemandeRecrutement demandeRecrutement) {
        this.demandeRecrutement = demandeRecrutement;
    }

    public String getTypeBesoin() {
        return typeBesoin;
    }

    public void setTypeBesoin(String typeBesoin) {
        this.typeBesoin = typeBesoin;
    }

    public Poste getPoste() {
        return poste;
    }

    public void setPoste(Poste poste) {
        this.poste = poste;
    }

    public Integer getNombrePostes() {
        return nombrePostes;
    }

    public void setNombrePostes(Integer nombrePostes) {
        this.nombrePostes = nombrePostes;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public LocalDate getDateEssaiSouhaitee() {
        return dateEssaiSouhaitee;
    }

    public void setDateEssaiSouhaitee(LocalDate dateEssaiSouhaitee) {
        this.dateEssaiSouhaitee = dateEssaiSouhaitee;
    }

    public BigDecimal getSalairePropose() {
        return salairePropose;
    }

    public void setSalairePropose(BigDecimal salairePropose) {
        this.salairePropose = salairePropose;
    }

    public StatutLigne getStatutLigne() {
        return statutLigne;
    }

    public void setStatutLigne(StatutLigne statutLigne) {
        this.statutLigne = statutLigne;
    }

    public List<HistoriqueValidationLigne> getHistoriques() {
        return historiques;
    }

    public void setHistoriques(List<HistoriqueValidationLigne> historiques) {
        this.historiques = historiques;
    }

}
