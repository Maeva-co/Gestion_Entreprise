package com.entreprise.gestion_entreprise.dto;

import com.entreprise.gestion_entreprise.config.EtapeValidation;

/**
 * Où en est une ligne de recrutement dans le circuit de validation.
 * Les libellés, couleurs et onglets sont déduits des étapes configurées dans
 * CircuitValidation : rien n'est à modifier ici quand le circuit change.
 */
public final class StatutSuivi {

    public enum Type {
        A_VALIDER, VALIDEE, REFUSEE
    }

    public static final String FILTRE_VALIDEE = "VALIDEE";
    public static final String FILTRE_REFUSEE = "REFUSEE";
    private static final String PREFIXE_A_VALIDER = "A_VALIDER_";

    private final Type type;
    private final String etapeCode;
    private final String etapeLibelle;
    private final String libelle;
    private final String couleur;
    private final String filtre;

    private StatutSuivi(Type type, String etapeCode, String etapeLibelle,
            String libelle, String couleur, String filtre) {
        this.type = type;
        this.etapeCode = etapeCode;
        this.etapeLibelle = etapeLibelle;
        this.libelle = libelle;
        this.couleur = couleur;
        this.filtre = filtre;
    }

    /** La ligne attend la décision de cette étape. */
    public static StatutSuivi aValider(EtapeValidation etape) {
        return new StatutSuivi(Type.A_VALIDER, etape.getCode(), etape.getLibelle(),
                "À valider " + etape.getLibelle(), etape.getCouleur(), filtreAValider(etape));
    }

    /** Toutes les étapes ont validé. */
    public static StatutSuivi validee() {
        return new StatutSuivi(Type.VALIDEE, null, null, "Validée", "emerald", FILTRE_VALIDEE);
    }

    /** La ligne a été refusée à cette étape. */
    public static StatutSuivi refusee(EtapeValidation etape) {
        return new StatutSuivi(Type.REFUSEE, etape.getCode(), etape.getLibelle(),
                "Refusée par " + etape.getLibelle(), "rose", FILTRE_REFUSEE);
    }

    /** Code de l'onglet "À valider X" pour une étape. */
    public static String filtreAValider(EtapeValidation etape) {
        return PREFIXE_A_VALIDER + etape.getCode();
    }

    /** Vrai si la ligne attend la décision de l'étape donnée. */
    public boolean attend(String codeEtape) {
        return type == Type.A_VALIDER && etapeCode.equals(codeEtape);
    }

    /** Vrai si la ligne a été refusée par l'étape donnée. */
    public boolean estRefuseePar(String codeEtape) {
        return type == Type.REFUSEE && etapeCode.equals(codeEtape);
    }

    public boolean estRefusee() {
        return type == Type.REFUSEE;
    }

    public Type getType() {
        return type;
    }

    /** Code de l'étape concernée (nul si la ligne est validée). */
    public String getEtapeCode() {
        return etapeCode;
    }

    public String getEtapeLibelle() {
        return etapeLibelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getCouleur() {
        return couleur;
    }

    public String getFiltre() {
        return filtre;
    }
}