package com.gitech.bank.service;

import com.gitech.bank.dto.auth.AuthResponseDTO;
import com.gitech.bank.dto.auth.LoginRequestDTO;
import com.gitech.bank.dto.auth.RegisterRequestDTO;
import com.gitech.bank.entity.Role;
import com.gitech.bank.entity.User;
import com.gitech.bank.exception.DuplicateResourceException;
import com.gitech.bank.exception.InvalidCredentialsException;
import com.gitech.bank.repository.UserRepository;
import com.gitech.bank.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remarque pédagogique : ce service N'UTILISE PAS `AuthenticationManager` /
 * `AuthenticationProvider` (les briques "officielles" de Spring Security
 * pour authentifier un login/mot de passe). C'est un choix délibéré pour
 * ce cours : en implémentant la vérification "à la main"
 * (`passwordEncoder.matches(...)`), le déroulé complet de l'authentification
 * reste visible et compréhensible pour un étudiant, sans machinerie cachée.
 * En production, on pourrait tout à fait relier ce service à un
 * `AuthenticationManager` configuré avec `CustomUserDetailsService` — le
 * résultat serait strictement équivalent.
 */
@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponseDTO register(RegisterRequestDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException("Ce nom d'utilisateur est déjà utilisé : " + dto.getUsername());
        }

        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                // Le mot de passe n'est JAMAIS stocké tel quel : passwordEncoder.encode()
                // applique BCrypt avant l'enregistrement en base.
                .password(passwordEncoder.encode(dto.getPassword()))
                // Toute inscription "libre" via /auth/register crée un simple
                // utilisateur : il n'existe volontairement AUCUN moyen de
                // s'auto-promouvoir ADMIN depuis cet endpoint public. La
                // création d'administrateurs se fait via DataInitializer
                // (ou, en production, par un administrateur existant).
                .role(Role.USER)
                .enabled(true)
                .build();

        User enregistre = userRepository.save(user);
        return construireReponse(enregistre);
    }

    public AuthResponseDTO login(LoginRequestDTO dto) {
        User user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Identifiants invalides"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            // Même message que "utilisateur introuvable" ci-dessus : on ne
            // révèle jamais lequel des deux (nom d'utilisateur ou mot de
            // passe) était incorrect.
            throw new InvalidCredentialsException("Identifiants invalides");
        }

        return construireReponse(user);
    }

    private AuthResponseDTO construireReponse(User user) {
        String token = jwtService.genererToken(user);
        return AuthResponseDTO.builder()
                .token(token)
                .type("Bearer")
                .username(user.getUsername())
                .role(user.getRole())
                .expiresInMs(jwtService.getExpirationMs())
                .build();
    }
}
