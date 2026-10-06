package com.gitech.bank.security;

import com.gitech.bank.entity.User;
import com.gitech.bank.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security ne connaît rien de notre entité JPA {@link User} : il
 * manipule uniquement l'interface `UserDetails`. Ce service fait donc le
 * pont entre les deux mondes, en chargeant un `User` depuis la base et en
 * le transformant en `UserDetails` que le framework sait comprendre
 * (utilisé par JwtAuthenticationFilter à CHAQUE requête authentifiée pour
 * reconstituer l'identité de l'utilisateur à partir de son jeton).
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + username));

        // Spring Security exige que chaque rôle soit préfixé par "ROLE_"
        // pour que hasRole("ADMIN") (utilisé dans @PreAuthorize) fonctionne.
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .disabled(!user.isEnabled())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .build();
    }
}
