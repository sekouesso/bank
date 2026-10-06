package com.gitech.bank.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Déclenché quand un utilisateur AUTHENTIFIÉ, mais n'ayant pas le bon rôle,
 * accède à une route protégée par `@PreAuthorize("hasRole('ADMIN')")` (voir
 * BankController). Contrairement à `RestAuthenticationEntryPoint` (401 :
 * "je ne sais pas qui vous êtes"), ce cas signifie "je sais qui vous êtes,
 * mais vous n'avez pas le droit" (403).
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String corps = """
                {"status":403,"error":"Forbidden","message":"Vous n'avez pas les droits nécessaires pour effectuer cette action.","path":"%s"}""".formatted(request.getRequestURI());

        response.getWriter().write(corps);
    }
}
