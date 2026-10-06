package com.gitech.bank.controller;

import com.gitech.bank.dto.auth.AuthResponseDTO;
import com.gitech.bank.dto.auth.LoginRequestDTO;
import com.gitech.bank.dto.auth.RegisterRequestDTO;
import com.gitech.bank.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur d'authentification. Ces deux routes (`/register` et `/login`)
 * sont les SEULES de l'application accessibles sans jeton JWT (voir
 * `SecurityConfig.securityFilterChain()` : `.requestMatchers("/api/v1/auth/**").permitAll()`)
 * — ce qui est logique : il faut bien pouvoir s'inscrire/se connecter
 * AVANT d'obtenir un jeton.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO dto) {
        return new ResponseEntity<>(authService.register(dto), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }
}
