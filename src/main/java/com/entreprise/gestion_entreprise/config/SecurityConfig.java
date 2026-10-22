package com.entreprise.gestion_entreprise.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Sécurité de l'application.
 *
 * La connexion est gérée "à la main" par AuthController (AuthService vérifie
 * l'email et le mot de passe BCrypt, puis le contrôleur place l'authentification
 * avec l'autorité ROLE_<code du rôle> dans la session). Cette classe se contente
 * donc de protéger les URL selon le rôle. La redirection vers la page d'accueil
 * selon le rôle est faite par AuthController#pageAccueil.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Injecté dans AuthController pour enregistrer l'authentification en session. */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, SecurityContextRepository contextRepository)
            throws Exception {
        http
                // Même repository que celui utilisé par AuthController
                .securityContext(c -> c.securityContextRepository(contextRepository))

                .authorizeHttpRequests(auth -> auth
                        // pages publiques
                        .requestMatchers("/", "/connexion", "/deconnexion", "/error",
                                "/candidate-portal/**","/acces-refuse", "/css/**", "/js/**", "/image/**")
                        .permitAll()

                        // une interface accessible à un seul rôle
                        .requestMatchers("/demandes-recrutement/validation/rh/**").hasAnyRole("RH","DG")
                        .requestMatchers("/demandes-recrutement/validation/dg/**").hasRole("DG")

                        // pour autoriser plusieurs rôles : hasAnyRole("RH", "MANAGER")
                        .requestMatchers("/demandes-recrutement/nouvelle/**").hasRole("RH")

                        // tout le reste : utilisateur connecté (ex. /acces-refuse)
                        .anyRequest().authenticated())

                // La connexion et la déconnexion sont gérées par AuthController
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                .exceptionHandling(e -> e
                        // non connecté -> page de connexion (sinon Spring renvoie une erreur 403)
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/connexion"))
                        // connecté mais rôle insuffisant -> message affiché par AuthController
                        .accessDeniedHandler((req, res, ex) ->
                                res.sendRedirect(req.getContextPath() + "/acces-refuse")));

        return http.build();
    }
}