package com.entreprise.gestion_entreprise.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Circuit de validation des lignes de recrutement, lu dans application.yml :
 *
 * validation:
 *   circuit:
 *     decisive: DG          # optionnel : par défaut, la dernière étape
 *     etapes:
 *       - code: RH
 *         ...
 *
 * L'ordre de la liste est l'ordre de validation. L'étape « décisive » a le
 * dernier mot : elle peut trancher une ligne refusée plus tôt, et c'est sa
 * validation qui transforme la ligne en offre d'emploi.
 */
@Component
@ConfigurationProperties(prefix = "validation.circuit")
public class CircuitValidation {

    private List<EtapeValidation> etapes = new ArrayList<>();

    /** Code de l'étape décisive. Si absent, c'est la dernière étape de la liste. */
    private String decisive;

    /** Vérifie la configuration au démarrage : une erreur empêche l'application de démarrer. */
    @PostConstruct
    void valider() {
        if (etapes == null || etapes.isEmpty()) {
            throw new IllegalStateException(
                    "Aucune étape de validation configurée. "
                            + "Ajoute 'validation.circuit.etapes' dans application.yml.");
        }

        Set<String> codes = new HashSet<>();
        Set<String> roles = new HashSet<>();

        for (EtapeValidation etape : etapes) {
            if (estVide(etape.getCode())) {
                throw new IllegalStateException("Une étape de validation n'a pas de 'code'.");
            }
            if (etape.getCode().length() > 50) {
                throw new IllegalStateException(
                        "Le code d'étape '" + etape.getCode() + "' dépasse 50 caractères.");
            }
            if (estVide(etape.getRoleCode())) {
                throw new IllegalStateException(
                        "L'étape '" + etape.getCode() + "' n'a pas de 'role-code'.");
            }
            if (estVide(etape.getLibelle())) {
                etape.setLibelle(etape.getCode());
            }
            if (!codes.add(etape.getCode())) {
                throw new IllegalStateException("Étape en double : '" + etape.getCode() + "'.");
            }
            if (!roles.add(etape.getRoleCode())) {
                throw new IllegalStateException("Le rôle '" + etape.getRoleCode()
                        + "' est associé à plusieurs étapes : un rôle ne peut valider qu'une seule étape.");
            }
        }

        if (!estVide(decisive) && parCode(decisive).isEmpty()) {
            throw new IllegalStateException(
                    "L'étape décisive '" + decisive + "' n'existe pas dans la liste des étapes.");
        }
    }

    // ---------- Accès au circuit ----------

    /** Étape décisive : celle configurée, sinon la dernière du circuit. */
    public EtapeValidation getEtapeDecisive() {
        if (etapes == null || etapes.isEmpty()) {
            return null;
        }
        EtapeValidation derniere = etapes.get(etapes.size() - 1);
        if (estVide(decisive)) {
            return derniere;
        }
        return parCode(decisive).orElse(derniere);
    }

    public boolean estDecisive(String codeEtape) {
        EtapeValidation decisiveEtape = getEtapeDecisive();
        return decisiveEtape != null && decisiveEtape.getCode().equals(codeEtape);
    }

    public Optional<EtapeValidation> parCode(String codeEtape) {
        if (codeEtape == null) {
            return Optional.empty();
        }
        return etapes.stream().filter(e -> codeEtape.equals(e.getCode())).findFirst();
    }

    /** L'étape qu'un rôle a le droit de valider. */
    public Optional<EtapeValidation> parRole(String roleCode) {
        if (roleCode == null) {
            return Optional.empty();
        }
        return etapes.stream().filter(e -> roleCode.equals(e.getRoleCode())).findFirst();
    }

    /**
     * Codes des rôles qui interviennent dans le circuit.
     * Pratique pour la sécurité : .hasAnyRole(circuit.rolesDesEtapes())
     */
    public String[] rolesDesEtapes() {
        return etapes.stream().map(EtapeValidation::getRoleCode).toArray(String[]::new);
    }

    private static boolean estVide(String valeur) {
        return valeur == null || valeur.isBlank();
    }

    // ---------- Liaison avec application.yml ----------

    public List<EtapeValidation> getEtapes() {
        return etapes;
    }

    public void setEtapes(List<EtapeValidation> etapes) {
        this.etapes = etapes;
    }

    public String getDecisive() {
        return decisive;
    }

    public void setDecisive(String decisive) {
        this.decisive = decisive;
    }
}