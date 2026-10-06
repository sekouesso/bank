package com.gitech.bank.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Déclenché quand une requête NON authentifiée tente d'accéder à une route
 * protégée (pas de jeton, ou jeton invalide/expiré non rattrapé par
 * JwtAuthenticationFilter).
 *
 * Point pédagogique important : SANS ce composant, Spring Security
 * renverrait par défaut un 403 "brut" (corps de réponse vide) — et ce cas
 * ne passerait PAS par `GlobalExceptionHandler` (@RestControllerAdvice),
 * car il se produit dans la CHAÎNE DE FILTRES, avant même que la requête
 * n'atteigne le `DispatcherServlet` (donc avant tout contrôleur). On
 * construit ici la réponse JSON "à la main" (sans passer par un
 * ObjectMapper Jackson) pour rester totalement indépendant de la version
 * de Jackson utilisée par le projet.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        ecrireErreur(response, request.getRequestURI());
    }

    private void ecrireErreur(HttpServletResponse response, String chemin) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String corps = """
                {"status":401,"error":"Unauthorized","message":"Authentification requise : jeton manquant, invalide ou expiré.","path":"%s"}""".formatted(chemin);

        response.getWriter().write(corps);
    }
}
