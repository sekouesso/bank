package com.gitech.bank.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ===========================================================================
 *  COURS : le filtre d'authentification JWT
 * ===========================================================================
 * `OncePerRequestFilter` garantit que ce filtre s'exécute UNE SEULE fois
 * par requête HTTP (utile en cas de forward/include internes). Il est
 * inséré dans la chaîne de filtres de Spring Security via
 * `SecurityConfig.securityFilterChain()` :
 *
 *     .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
 *
 * Déroulé pour CHAQUE requête entrante :
 *   1) Lire l'en-tête HTTP `Authorization: Bearer <token>`.
 *   2) Si absent → laisser passer la requête SANS authentifier personne
 *      (c'est `authorizeHttpRequests` qui décidera ensuite si la route
 *      demandée exige d'être authentifié ou non).
 *   3) Si présent → extraire le nom d'utilisateur du jeton, recharger ses
 *      autorités via CustomUserDetailsService, puis peupler le
 *      `SecurityContextHolder` avec un `UsernamePasswordAuthenticationToken`
 *      : c'est CE token que Spring Security consultera ensuite pour
 *      répondre à `@PreAuthorize("hasRole('ADMIN')")` ou à
 *      `.anyRequest().authenticated()`.
 *
 * Remarque : on n'utilise PAS `AuthenticationManager` ici (contrairement à
 * un login classique par formulaire) : la validation du jeton EST la
 * preuve d'authentification, il n'y a pas de mot de passe à revérifier à
 * chaque requête.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String EN_TETE_AUTORISATION = "Authorization";
    private static final String PREFIXE_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String enTete = request.getHeader(EN_TETE_AUTORISATION);

        if (enTete == null || !enTete.startsWith(PREFIXE_BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = enTete.substring(PREFIXE_BEARER.length());

        try {
            String username = jwtService.extraireUsername(token);

            // On ne réauthentifie pas si le SecurityContext contient déjà
            // une authentification (cas de forwards internes notamment).
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.estValide(token, userDetails.getUsername())) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            // Jeton invalide, expiré, ou utilisateur supprimé entre-temps :
            // on ne bloque PAS la requête ici (on ne renvoie pas d'erreur
            // depuis un filtre). On se contente de NE PAS authentifier
            // l'utilisateur ; c'est `authorizeHttpRequests` puis
            // `RestAuthenticationEntryPoint` qui décideront de renvoyer un
            // 401 si la route demandée exige d'être authentifié.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
