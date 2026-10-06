package com.gitech.bank.security;

import com.gitech.bank.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * ===========================================================================
 *  COURS : génération et validation d'un jeton JWT (JSON Web Token)
 * ===========================================================================
 * Un JWT est composé de 3 parties encodées en Base64Url et séparées par un
 * point : `en-tête.charge_utile.signature`.
 *   - l'EN-TÊTE précise l'algorithme de signature utilisé (ici HS256) ;
 *   - la CHARGE UTILE ("claims") contient les informations sur
 *     l'utilisateur (ici : son nom d'utilisateur en "subject", son rôle, et
 *     une date d'expiration) — ATTENTION : ces informations sont
 *     SEULEMENT encodées, PAS chiffrées ! N'y mettez jamais de données
 *     sensibles (mot de passe, etc.), n'importe qui peut décoder un JWT.
 *   - la SIGNATURE prouve que le jeton a bien été émis par notre serveur
 *     et n'a pas été altéré : elle est calculée avec une clé secrète que
 *     SEUL le serveur connaît (voir `jwt.secret` dans application.yml).
 *
 * Le jeton est donc "auto-porteur" (stateless) : le serveur n'a pas besoin
 * de garder de session en mémoire ou en base pour savoir qui est
 * l'utilisateur, il lui suffit de revalider la signature à chaque requête
 * (voir JwtAuthenticationFilter).
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    /** Construit un nouveau jeton signé pour l'utilisateur donné. */
    public String genererToken(User user) {
        Date maintenant = new Date();
        Date expiration = new Date(maintenant.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .claim("userId", user.getId())
                .issuedAt(maintenant)
                .expiration(expiration)
                .signWith(cleSignature())
                .compact();
    }

    /** Extrait le nom d'utilisateur ("subject") porté par le jeton. */
    public String extraireUsername(String token) {

        return extraireClaim(token, Claims::getSubject);
    }

    /**
     * Un jeton est valide s'il correspond bien à l'utilisateur attendu ET
     * qu'il n'est pas expiré. La vérification de la SIGNATURE, elle, est
     * faite implicitement par `Jwts.parser().verifyWith(...)` : si la
     * signature ne correspond pas (jeton falsifié), une JwtException est
     * levée AVANT même d'arriver ici (voir extraireClaim).
     */
    public boolean estValide(String token, String usernameAttendu) {
        String username = extraireUsername(token);
        return username.equals(usernameAttendu) && !estExpire(token);
    }

    public long getExpirationMs() {

        return expirationMs;
    }

    private boolean estExpire(String token) {

        return extraireClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extraireClaim(String token, Function<Claims, T> resolveur) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(cleSignature())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return resolveur.apply(claims);
        } catch (JwtException | IllegalArgumentException e) {
            // Jeton malformé, expiré, ou signature invalide : on relance une
            // exception générique que le filtre saura gérer (voir
            // JwtAuthenticationFilter), sans jamais faire confiance au jeton.
            throw new JwtException("Jeton JWT invalide : " + e.getMessage(), e);
        }
    }

    private SecretKey cleSignature() {
        byte[] octetsCle = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(octetsCle);
    }
}
