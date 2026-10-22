package com.entreprise.gestion_entreprise.config;

/**
 * Une étape du circuit de validation d'une ligne de recrutement.
 * Chaque étape est déclarée dans application.yml (validation.circuit.etapes) :
 * rien n'est à modifier dans le code quand on ajoute, retire ou réordonne une étape.
 */
public class EtapeValidation {

    /** Code stocké dans historique_validation_ligne.etape (50 caractères max). Ex. : RH. */
    private String code;

    /** Libellé affiché dans l'interface. Par défaut : le code. */
    private String libelle;

    /** Code du rôle (table role) autorisé à valider cette étape. Un rôle = une étape. */
    private String roleCode;

    /** Couleur utilisée pour les badges « À valider ». */
    private String couleur = "amber";

    public EtapeValidation() {
    }

    public EtapeValidation(String code, String libelle, String roleCode, String couleur) {
        this.code = code;
        this.libelle = libelle;
        this.roleCode = roleCode;
        if (couleur != null) {
            this.couleur = couleur;
        }
    }

    public String getCode() {
        return code;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    @Override
    public String toString() {
        return "EtapeValidation[" + code + ", rôle " + roleCode + "]";
    }
}