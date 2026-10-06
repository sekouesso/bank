package com.gitech.bank.dto.auth;

import com.gitech.bank.entity.Role;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDTO {

    private String token;

    /** Toujours "Bearer" ici : indique au client comment utiliser le jeton (header "Authorization: Bearer <token>"). */
    private String type;

    private String username;
    private Role role;

    /** Durée de validité du jeton en millisecondes, pour que le front puisse anticiper l'expiration. */
    private long expiresInMs;
}
