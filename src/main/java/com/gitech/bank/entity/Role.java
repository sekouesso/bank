package com.gitech.bank.entity;

/**
 * Rôles applicatifs. Spring Security exige par convention le préfixe
 * "ROLE_" au moment de construire les autorités (voir
 * CustomUserDetailsService), mais on le stocke ici SANS préfixe pour rester
 * lisible en base de données.
 */
public enum Role {
    ADMIN,
    USER
}
