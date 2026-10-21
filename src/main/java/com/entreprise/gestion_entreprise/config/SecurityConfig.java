package com.entreprise.gestion_entreprise.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration TEMPORAIRE de développement : aucune page de connexion,
 * toutes les URL sont accessibles. À remplacer par une vraie authentification.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    SecurityContextRepository securityContextRepository) throws Exception {
        http
                .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/connexion", "/deconnexion", "/css/**", "/js/**", "/favicon.ico")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/connexion")));
        return http.build();
    }
}