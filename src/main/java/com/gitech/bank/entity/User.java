package com.gitech.bank.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Compte utilisateur applicatif, distinct de l'entité métier {@link Client}.
 * Un `User` sert UNIQUEMENT à s'authentifier (login/mot de passe/rôle) ; il
 * n'est volontairement pas relié à un `Client` dans ce projet pédagogique,
 * pour garder la démonstration de l'authentification JWT indépendante du
 * reste du modèle métier.
 */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    private String email;

    /**
     * Mot de passe TOUJOURS stocké haché (BCrypt), jamais en clair. Le
     * hachage est effectué dans AuthService via le bean PasswordEncoder
     * (voir SecurityConfig), jamais dans l'entité elle-même.
     */
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Builder.Default
    private boolean enabled = true;
}
