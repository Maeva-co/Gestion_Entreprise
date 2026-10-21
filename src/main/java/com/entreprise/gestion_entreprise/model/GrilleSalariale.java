package com.entreprise.gestion_entreprise.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "grille_salariale")
public class GrilleSalariale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String niveau;

    @Column(name = "salaire_embauche", nullable = false, precision = 12, scale = 2)
    private BigDecimal salaireEmbauche;

    @Column(name = "salaire_anciennete", nullable = false, precision = 12, scale = 2)
    private BigDecimal salaireAnciennete;

    @Column(name = "salaire_revalorise", nullable = false, precision = 12, scale = 2)
    private BigDecimal salaireRevalorise;

    public Integer getId() {
        return id;
    }

    public String getNiveau() {
        return niveau;
    }

    public BigDecimal getSalaireEmbauche() {
        return salaireEmbauche;
    }

    public BigDecimal getSalaireAnciennete() {
        return salaireAnciennete;
    }

    public BigDecimal getSalaireRevalorise() {
        return salaireRevalorise;
    }


}
