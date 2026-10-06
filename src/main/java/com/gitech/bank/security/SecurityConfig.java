package com.gitech.bank.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * ===========================================================================
 *  COURS : configuration centrale de Spring Security (Spring Security 7 /
 *  Spring Boot 4 — API 100 % "lambda DSL", l'ancien style `.and()` a été
 *  supprimé dans cette version).
 * ===========================================================================
 *
 * Points clés à expliquer aux étudiants :
 *
 * 1) `@EnableMethodSecurity` active les annotations `@PreAuthorize` /
 *    `@PostAuthorize` posées directement sur les méthodes des contrôleurs
 *    (voir BankController : `@PreAuthorize("hasRole('ADMIN')")`). C'est un
 *    SECOND niveau de protection, complémentaire à `authorizeHttpRequests`
 *    ci-dessous (qui protège au niveau de l'URL).
 *
 * 2) `csrf().disable()` : la protection CSRF protège les applications à
 *    base de SESSIONS et de cookies (un attaquant pourrait forger une
 *    requête depuis un autre site, le navigateur enverrait automatiquement
 *    le cookie de session). Notre API est STATELESS et s'authentifie par
 *    un en-tête `Authorization: Bearer <token>` que le navigateur n'envoie
 *    JAMAIS automatiquement : le risque CSRF ne s'applique donc pas ici.
 *    ⚠️ Depuis Spring Security 7, le CSRF est activé par défaut même sur
 *    les API : il FAUT le désactiver explicitement, sous peine de voir
 *    tous les POST/PUT/DELETE renvoyer un 403.
 *
 * 3) `sessionCreationPolicy(STATELESS)` : on interdit explicitement à
 *    Spring Security de créer une `HttpSession`. Chaque requête doit se
 *    suffire à elle-même (son jeton JWT) — c'est le principe même d'une
 *    API REST stateless.
 *
 * 4) `authorizeHttpRequests` : la liste des règles est évaluée dans
 *    l'ordre, la PREMIÈRE règle qui correspond à l'URL demandée
 *    s'applique. `/api/v1/auth/**` (inscription/connexion) et la console
 *    H2 doivent être accessibles SANS jeton (on ne peut pas exiger un
 *    jeton pour... obtenir un jeton !). Tout le reste exige d'être
 *    authentifié.
 *
 * 5) `addFilterBefore(jwtAuthenticationFilter, ...)` : insère notre filtre
 *    JWT personnalisé JUSTE AVANT le filtre standard de Spring Security
 *    qui gère l'authentification par formulaire — dans notre cas ce
 *    dernier n'est jamais déclenché (pas de `.formLogin()`), mais c'est le
 *    point d'ancrage conventionnel pour un filtre d'authentification.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint,
            RestAccessDeniedHandler restAccessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.restAccessDeniedHandler = restAccessDeniedHandler;
    }

    /**
     * BCrypt est un algorithme de hachage à sens unique et volontairement
     * lent (pour ralentir les attaques par force brute), avec un "sel"
     * aléatoire intégré à chaque hachage : deux appels sur le même mot de
     * passe produisent deux résultats différents, mais `.matches(...)`
     * sait néanmoins les comparer correctement.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // requêtes "preflight" CORS
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/swagger-ui.html/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(restAuthenticationEntryPoint) // 401
                        .accessDeniedHandler(restAccessDeniedHandler)           // 403
                )
                // Nécessaire uniquement pour que la console H2 (qui s'affiche
                // dans une <frame>) ne soit pas bloquée par la protection
                // anti-clickjacking par défaut de Spring Security.
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS est désormais géré ICI (au niveau de Spring Security), et non
     * plus dans un `WebMvcConfigurer` classique (voir l'ancien
     * `WebConfig`, supprimé) : la chaîne de filtres de Spring Security
     * s'exécute AVANT le `DispatcherServlet`, donc avant toute
     * configuration CORS déclarée uniquement côté MVC.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
