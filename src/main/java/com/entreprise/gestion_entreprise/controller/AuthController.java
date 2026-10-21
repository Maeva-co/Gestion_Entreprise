package com.entreprise.gestion_entreprise.controller;

import com.entreprise.gestion_entreprise.dto.ConnexionRequest;
import com.entreprise.gestion_entreprise.exception.RegleGestionException;
import com.entreprise.gestion_entreprise.model.Utilisateur;
import com.entreprise.gestion_entreprise.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

@Controller
public class AuthController {

    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(AuthService authService,
                          SecurityContextRepository securityContextRepository) {
        this.authService = authService;
        this.securityContextRepository = securityContextRepository;
    }

    /** Affiche la page de connexion. */
    @GetMapping("/connexion")
    public String afficherConnexion(Model model,
                                    @RequestParam(value = "error", required = false) String error) {
        model.addAttribute("connexionRequest", new ConnexionRequest());
        if (error != null) {
            model.addAttribute("connexionError", "Identifiants invalides. Veuillez réessayer.");
        }
        return "connexion";
    }

    /** Traite la soumission du formulaire de connexion. */
    @PostMapping("/connexion")
    public String connecter(@ModelAttribute ConnexionRequest connexionRequest,
                            HttpServletRequest request,
                            HttpServletResponse response,
                            Model model) {
        try {
            Utilisateur utilisateur = authService.authentifier(
                    connexionRequest.getEmail(), connexionRequest.getMotDePasse());

            // Crée la session si besoin, puis change son id (protection contre la fixation de session)
            request.getSession(true);
            request.changeSessionId();

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    utilisateur.getEmail(), null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + utilisateur.getRole().getCode())));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response); // persiste en session

            return "redirect:/demandes-recrutement/nouvelle";
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