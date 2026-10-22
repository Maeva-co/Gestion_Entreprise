package com.entreprise.gestion_entreprise.controller;

import com.entreprise.gestion_entreprise.dto.ConnexionRequest;
import com.entreprise.gestion_entreprise.exception.RegleGestionException;
import com.entreprise.gestion_entreprise.model.Utilisateur;
import com.entreprise.gestion_entreprise.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class AuthController {

    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(AuthService authService,
            SecurityContextRepository securityContextRepository) {
        this.authService = authService;
        this.securityContextRepository = securityContextRepository;
    }

    private boolean estConnecte(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    // L'ordre = la priorité si un utilisateur cumule plusieurs rôles
    private static final List<Map.Entry<String, String>> ACCUEIL_PAR_ROLE = List.of(
            Map.entry("ROLE_ADMIN", "/demandes-recrutement/validation/rh/liste"),
            Map.entry("ROLE_DG", "/demandes-recrutement/validation/rh/liste"),
            Map.entry("ROLE_RH", "/demandes-recrutement/validation/rh/liste"),
            Map.entry("ROLE_CHEF_DEPARTEMENT", "/demandes-recrutement/nouvelle"),
            Map.entry("ROLE_FINANCE", "/demandes-recrutement/validation/rh/liste"));

    private String urlAccueil(Authentication auth) {
        Set<String> roles = AuthorityUtils.authorityListToSet(auth.getAuthorities());
        return ACCUEIL_PAR_ROLE.stream()
                .filter(e -> roles.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("/acces-refuse");
    }

    private String pageAccueil(Authentication auth) {
        return "redirect:" + urlAccueil(auth);
    }

    @GetMapping("/acces-refuse")
    public String accesRefuse(Authentication auth, Model model) {
        model.addAttribute("accueilUrl", urlAccueil(auth));
        return "acces-refuse";
    }

    /**
     * Affiche la page de connexion (ou renvoie l'utilisateur déjà connecté vers son
     * espace).
     */
    @GetMapping({ "/", "/connexion" })
    public String afficherConnexion(Model model, Authentication authentication,
            @RequestParam(value = "error", required = false) String error) {
        if (estConnecte(authentication)) {
            return pageAccueil(authentication);
        }
        model.addAttribute("connexionRequest", new ConnexionRequest());
        if (error != null) {
            model.addAttribute("connexionError",
                    "access_denied".equals(error)
                            ? "Vous n'avez pas les droits pour accéder à cette page."
                            : "Identifiants invalides. Veuillez réessayer.");
        }
        return "connexion";
    }

    @PostMapping("/connexion")
    public String connecter(@ModelAttribute ConnexionRequest connexionRequest,
            HttpServletRequest request, HttpServletResponse response,
            Model model, Authentication authentication) {
        if (estConnecte(authentication)) {
            return pageAccueil(authentication);
        }
        try {
            Utilisateur utilisateur = authService.authentifier(
                    connexionRequest.getEmail(), connexionRequest.getMotDePasse());

            request.getSession(true);
            request.changeSessionId();

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    utilisateur.getEmail(), null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + utilisateur.getRole().getCode())));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            return pageAccueil(auth);
        } catch (RegleGestionException e) {
            model.addAttribute("connexionError", e.getMessage());
            return "connexion";
        }
    }

    /** Déconnecte l'utilisateur. */
    @GetMapping("/deconnexion")
    public String deconnecter(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        return "redirect:/connexion";
    }

}