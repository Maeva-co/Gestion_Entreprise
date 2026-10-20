package com.entreprise.gestion_entreprise.service;

import com.entreprise.gestion_entreprise.model.*;
import com.entreprise.gestion_entreprise.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DemandeRecrutementService {

    /** Ids dans la table type_demande_recrutement. */
    public static final int TYPE_REMPLACEMENT = 1;
    public static final int TYPE_CREATION = 2;

    /** Valeurs de LigneDemandeRecrutement.typeBesoin. */
    private static final String BESOIN_REMPLACEMENT = "REMPLACEMENT";
    private static final String BESOIN_CREATION = "CREATION";

    /** Codes à adapter selon vos données de référence. */
    private static final String CODE_POSTE_VACANT = "VACANT";
    private static final String CODE_DEMANDE_INITIALE = "EN_ATTENTE";
    private static final String CODE_LIGNE_INITIALE = "EN_ATTENTE";

    // ---------- DTO (records) ----------
    public record PosteDto(Integer id, String titre) {}

    public record PosteVacantDto(Integer id, String titre, int effectifMax) {}

    public record BudgetDto(String niveau, BigDecimal salaireUnitaire) {}

    public record ProchainPosteDto(String titreMere, int prochainIndice, String titre, BudgetDto budget) {}

    public record LigneForm(Integer posteMereId, Integer posteId, Integer effectif) {}

    public record DemandeForm(Integer departementId, Integer typeId, List<LigneForm> lignes) {}

    private record RefCreation(int dernierIndice, GrilleSalariale grille) {}

    private final DemandeRecrutementRepository demandeRepository;
    private final DepartementRepository departementRepository;
    private final TypeDemandeRecrutementRepository typeRepository;
    private final StatutDemandeRepository statutDemandeRepository;
    private final StatutLigneRepository statutLigneRepository;
    private final PosteRepository posteRepository;

    public DemandeRecrutementService(DemandeRecrutementRepository demandeRepository,
                                     DepartementRepository departementRepository,
                                     TypeDemandeRecrutementRepository typeRepository,
                                     StatutDemandeRepository statutDemandeRepository,
                                     StatutLigneRepository statutLigneRepository,
                                     PosteRepository posteRepository) {
        this.demandeRepository = demandeRepository;
        this.departementRepository = departementRepository;
        this.typeRepository = typeRepository;
        this.statutDemandeRepository = statutDemandeRepository;
        this.statutLigneRepository = statutLigneRepository;
        this.posteRepository = posteRepository;
    }

    // ---------- Données du formulaire ----------

    @Transactional(readOnly = true)
    public List<Departement> getDepartements() {
        return departementRepository.findAll(Sort.by("nom"));
    }

    @Transactional(readOnly = true)
    public List<TypeDemandeRecrutement> getTypesDemande() {
        return typeRepository.findAll(Sort.by("id"));
    }

    /** Postes mères (sans parent) du département, ex : DEV, RH, COMPTA. */
    @Transactional(readOnly = true)
    public List<PosteDto> getPostesMeres(Integer departementId) {
        return posteRepository.findByDepartementIdAndPosteParentIsNullOrderByTitreAsc(departementId).stream()
                .map(p -> new PosteDto(p.getId(), p.getTitre()))
                .toList();
    }

    // ---------- Type 1 : remplacement ----------

    /**
     * Postes filles vacants d'un poste mère dans un département.
     * Les postes vacants de même titre sont regroupés : effectifMax = nombre de postes vacants.
     */
    @Transactional(readOnly = true)
    public List<PosteVacantDto> getPostesVacants(Integer departementId, Integer posteMereId) {
        List<Poste> vacants = posteRepository
                .findByDepartementIdAndStatutPosteCodeOrderByTitreAsc(departementId, CODE_POSTE_VACANT).stream()
                .filter(p -> estFilleDe(p, posteMereId))
                .toList();

        // La liste est déjà triée par titre : LinkedHashMap conserve cet ordre.
        Map<String, List<Poste>> parTitre = vacants.stream()
                .collect(Collectors.groupingBy(Poste::getTitre, LinkedHashMap::new, Collectors.toList()));

        return parTitre.values().stream()
                .map(liste -> new PosteVacantDto(liste.get(0).getId(), liste.get(0).getTitre(), liste.size()))
                .toList();
    }

    /** Budget (grille salariale) du poste mère. */
    @Transactional(readOnly = true)
    public BudgetDto getBudgetPosteMere(Integer posteMereId) {
        return toBudget(trouverPoste(posteMereId).getGrilleSalariale());
    }

    // ---------- Type 2 : création ----------

    /**
     * Prochain poste fille à créer (ex : DEV 3 existe -> DEV 4)
     * et budget issu de la grille du poste similaire déjà présent dans le département.
     */
    @Transactional(readOnly = true)
    public ProchainPosteDto getProchainPoste(Integer departementId, Integer posteMereId) {
        Poste mere = trouverMere(departementId, posteMereId);
        RefCreation ref = referenceCreation(departementId, mere);
        int indice = ref.dernierIndice() + 1;
        return new ProchainPosteDto(mere.getTitre(), indice, mere.getTitre() + " " + indice, toBudget(ref.grille()));
    }

    // ---------- Enregistrement ----------

    @Transactional
    public DemandeRecrutement creer(DemandeForm form, Utilisateur demandeur) {
        if (form == null || form.departementId() == null || form.typeId() == null) {
            throw new IllegalArgumentException("Le département et le type de demande sont obligatoires.");
        }
        if (form.lignes() == null || form.lignes().isEmpty()) {
            throw new IllegalArgumentException("Ajoutez au moins une ligne à la demande.");
        }

        Departement departement = departementRepository.findById(form.departementId())
                .orElseThrow(() -> new IllegalArgumentException("Département introuvable."));
        TypeDemandeRecrutement type = typeRepository.findById(form.typeId())
                .orElseThrow(() -> new IllegalArgumentException("Type de demande introuvable."));
        StatutDemande statutDemande = statutDemandeRepository.findByCode(CODE_DEMANDE_INITIALE)
                .orElseThrow(() -> new IllegalStateException("Statut de demande '" + CODE_DEMANDE_INITIALE + "' absent de la base."));
        StatutLigne statutLigne = statutLigneRepository.findByCode(CODE_LIGNE_INITIALE)
                .orElseThrow(() -> new IllegalStateException("Statut de ligne '" + CODE_LIGNE_INITIALE + "' absent de la base."));

        DemandeRecrutement demande = new DemandeRecrutement();
        demande.setDemandeur(demandeur);
        demande.setDepartement(departement);
        demande.setStatutDemande(statutDemande);

        if (type.getId() == TYPE_REMPLACEMENT) {
            ajouterLignesRemplacement(demande, departement, form.lignes(), statutLigne);
        } else if (type.getId() == TYPE_CREATION) {
            ajouterLignesCreation(demande, departement, form.lignes(), statutLigne);
        } else {
            throw new IllegalArgumentException("Type de demande non géré.");
        }
        return demandeRepository.save(demande);
    }

    private void ajouterLignesRemplacement(DemandeRecrutement demande, Departement dep,
                                           List<LigneForm> lignes, StatutLigne statutLigne) {
        Map<String, Integer> cumulParTitre = new HashMap<>();

        for (LigneForm l : lignes) {
            validerLigne(l);
            if (l.posteId() == null) {
                throw new IllegalArgumentException("Choisissez le poste à remplacer sur chaque ligne.");
            }
            Poste mere = trouverMere(dep.getId(), l.posteMereId());
            Poste fille = trouverPoste(l.posteId());

            boolean valide = fille.getDepartement() != null && fille.getDepartement().getId().equals(dep.getId())
                    && estFilleDe(fille, mere.getId())
                    && fille.getStatutPoste() != null && CODE_POSTE_VACANT.equals(fille.getStatutPoste().getCode());
            if (!valide) {
                throw new IllegalArgumentException("Le poste « " + fille.getTitre() + " » n'est pas vacant dans ce département.");
            }

            int max = getPostesVacants(dep.getId(), mere.getId()).stream()
                    .filter(p -> p.titre().equals(fille.getTitre()))
                    .mapToInt(PosteVacantDto::effectifMax)
                    .findFirst().orElse(0);
            int total = cumulParTitre.merge(fille.getTitre(), l.effectif(), Integer::sum);
            if (total > max) {
                throw new IllegalArgumentException("Effectif demandé pour « " + fille.getTitre()
                        + " » (" + total + ") supérieur au nombre de postes vacants (" + max + ").");
            }

            // Budget : grille salariale du poste mère.
            ajouterLigne(demande, BESOIN_REMPLACEMENT, fille, l.effectif(), null,
                    mere.getGrilleSalariale(), statutLigne);
        }
    }

    private void ajouterLignesCreation(DemandeRecrutement demande, Departement dep,
                                       List<LigneForm> lignes, StatutLigne statutLigne) {
        // Plusieurs lignes pour la même mère : DEV 4, DEV 5, ...
        Map<Integer, Integer> compteurParMere = new HashMap<>();

        for (LigneForm l : lignes) {
            validerLigne(l);
            Poste mere = trouverMere(dep.getId(), l.posteMereId());
            RefCreation ref = referenceCreation(dep.getId(), mere);

            int rang = compteurParMere.merge(mere.getId(), 1, Integer::sum);
            String titre = mere.getTitre() + " " + (ref.dernierIndice() + rang);

            // Le poste à créer n'existe pas encore : la ligne pointe vers le poste mère
            // et le titre du futur poste est conservé dans le motif.
            ajouterLigne(demande, BESOIN_CREATION, mere, l.effectif(),
                    "Création du poste « " + titre + " »", ref.grille(), statutLigne);
        }
    }

    private void ajouterLigne(DemandeRecrutement demande, String typeBesoin, Poste poste, int effectif,
                              String motif, GrilleSalariale grille, StatutLigne statutLigne) {
        LigneDemandeRecrutement ligne = new LigneDemandeRecrutement();
        ligne.setTypeBesoin(typeBesoin);
        ligne.setPoste(poste);
        ligne.setNombrePostes(effectif);
        ligne.setMotif(motif);
        // Salaire unitaire issu de la grille, toujours recalculé côté serveur.
        ligne.setSalairePropose(toBudget(grille).salaireUnitaire());
        ligne.setStatutLigne(statutLigne);
        demande.addLigne(ligne);
    }

    // ---------- Utilitaires ----------

    private void validerLigne(LigneForm l) {
        if (l == null || l.posteMereId() == null) {
            throw new IllegalArgumentException("Choisissez un poste mère sur chaque ligne.");
        }
        if (l.effectif() == null || l.effectif() < 1) {
            throw new IllegalArgumentException("L'effectif doit être au moins égal à 1.");
        }
    }

    private Poste trouverPoste(Integer id) {
        return posteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Poste introuvable (id " + id + ")."));
    }

    /** Poste mère : sans parent et rattaché au département choisi. */
    private Poste trouverMere(Integer departementId, Integer posteMereId) {
        Poste mere = trouverPoste(posteMereId);
        boolean valide = mere.getPosteParent() == null
                && mere.getDepartement() != null
                && departementId.equals(mere.getDepartement().getId());
        if (!valide) {
            throw new IllegalArgumentException("Le poste mère « " + mere.getTitre() + " » n'appartient pas à ce département.");
        }
        return mere;
    }

    private boolean estFilleDe(Poste poste, Integer posteMereId) {
        return poste.getPosteParent() != null && posteMereId.equals(poste.getPosteParent().getId());
    }

    private BudgetDto toBudget(GrilleSalariale g) {
        if (g == null || g.getSalaireEmbauche() == null) {
            return new BudgetDto(g == null ? null : g.getNiveau(), BigDecimal.ZERO);
        }
        return new BudgetDto(g.getNiveau(), g.getSalaireEmbauche());
    }

    /**
     * Dernier indice utilisé par les postes filles du département (DEV 1, DEV 2, DEV 3 -> 3)
     * et grille du poste le plus récent, à défaut celle du poste mère.
     */
    private RefCreation referenceCreation(Integer departementId, Poste mere) {
        Poste referent = null;
        int max = 0;
        for (Poste f : posteRepository.findByPosteParentId(mere.getId())) {
            if (f.getDepartement() == null || !departementId.equals(f.getDepartement().getId())) {
                continue;
            }
            int idx = extraireIndice(f.getTitre(), mere.getTitre());
            if (referent == null || idx > max) {
                referent = f;
            }
            max = Math.max(max, idx);
        }
        GrilleSalariale grille = (referent != null && referent.getGrilleSalariale() != null)
                ? referent.getGrilleSalariale()
                : mere.getGrilleSalariale();
        return new RefCreation(max, grille);
    }

    private int extraireIndice(String titreFille, String titreMere) {
        Matcher m = Pattern.compile("^" + Pattern.quote(titreMere) + "\\s*(\\d+)$", Pattern.CASE_INSENSITIVE)
                .matcher(titreFille == null ? "" : titreFille.trim());
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }
}