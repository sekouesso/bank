# Bank API — Authentification JWT (cours)

Backend Spring Boot 4.1 / Spring Security 7 / Spring Data JPA. Ce document
complète le code source et explique, pour un cours, **comment fonctionne
l'authentification JWT** ajoutée à l'API `bank`.

## 1. Démarrage

```bash
./mvnw spring-boot:run
```

Au démarrage, `DataInitializer` crée automatiquement deux comptes de test :

| Utilisateur | Mot de passe | Rôle  |
|-------------|---------------|-------|
| `admin`     | `admin123`    | ADMIN |
| `user`      | `user123`     | USER  |

Console H2 : `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:bankdb`).

## 2. Vue d'ensemble des routes

| Méthode | URL                                | Accès                          |
|---------|-------------------------------------|---------------------------------|
| POST    | `/api/v1/auth/register`             | Public                          |
| POST    | `/api/v1/auth/login`                | Public                          |
| GET     | `/api/v1/clients`, `/clients/{id}`  | Authentifié (USER ou ADMIN)     |
| POST/PUT/DELETE | `/api/v1/clients...`        | **ADMIN uniquement**            |
| GET     | `/api/v1/accounts`, `/accounts/{id}`, `/clients/{id}/accounts` | Authentifié |
| POST/PUT/DELETE | `/api/v1/accounts...`       | **ADMIN uniquement**            |
| POST    | `/api/v1/accounts/{id}/depot`       | Authentifié                     |
| POST    | `/api/v1/accounts/{id}/retrait`     | Authentifié                     |
| POST    | `/api/v1/accounts/{id}/virement`    | Authentifié                     |

## 3. Se connecter et appeler l'API (exemples `curl`)

```bash
# 1) Connexion → récupère un jeton
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Réponse :
# { "token": "eyJhbGciOi...", "type": "Bearer", "username": "admin", "role": "ADMIN", "expiresInMs": 3600000 }

# 2) Utiliser le jeton pour appeler une route protégée
curl http://localhost:8080/api/v1/clients \
  -H "Authorization: Bearer eyJhbGciOi..."

# 3) Une route ADMIN avec un utilisateur "user" → 403 Forbidden
curl -X DELETE http://localhost:8080/api/v1/clients/1 \
  -H "Authorization: Bearer <jeton de 'user'>"
```

## 4. Anatomie du système JWT (package `com.gitech.bank.security`)

```
Requête HTTP
     │
     ▼
┌─────────────────────────────┐
│ JwtAuthenticationFilter     │  Lit l'en-tête Authorization, valide le jeton,
│ (OncePerRequestFilter)      │  peuple le SecurityContextHolder si valide
└──────────────┬──────────────┘
               ▼
┌─────────────────────────────┐
│ authorizeHttpRequests(...)  │  Vérifie que la route demandée est bien
│ (SecurityConfig)            │  accessible (permitAll / authenticated)
└──────────────┬──────────────┘
               ▼
┌─────────────────────────────┐
│ @PreAuthorize("hasRole...") │  Second filtrage, au niveau de la méthode
│ (BankController)            │  du contrôleur (ex: réservé à l'ADMIN)
└──────────────┬──────────────┘
               ▼
        Le contrôleur s'exécute
```

- **`JwtService`** — construit (`genererToken`) et valide (`estValide`) les
  jetons avec la bibliothèque JJWT. Le jeton contient le nom d'utilisateur
  (`subject`), le rôle et une date d'expiration — jamais de mot de passe.
- **`CustomUserDetailsService`** — recharge un utilisateur depuis la base
  (`UserRepository`) et le convertit au format `UserDetails` attendu par
  Spring Security.
- **`JwtAuthenticationFilter`** — s'exécute à CHAQUE requête, AVANT le
  contrôleur : il lit l'en-tête `Authorization: Bearer <token>` et, si le
  jeton est valide, authentifie la requête pour la durée de son traitement.
- **`SecurityConfig`** — assemble tout : CORS, désactivation CSRF (API
  stateless), routes publiques (`/auth/**`), et le filtre JWT inséré dans
  la chaîne.
- **`RestAuthenticationEntryPoint`** (401) / **`RestAccessDeniedHandler`**
  (403) — gèrent proprement, en JSON, les deux cas d'échec :
  "je ne sais pas qui vous êtes" vs "je sais qui vous êtes mais vous n'avez
  pas le droit".

## 5. Pourquoi deux niveaux de protection (URL + méthode) ?

`SecurityConfig` protège au niveau des **URL** (`anyRequest().authenticated()`)
: c'est une protection large et facile à auditer d'un coup d'œil (quelles
routes sont publiques ?). `@PreAuthorize` sur chaque méthode du contrôleur
protège au niveau **métier**, avec des règles plus fines (ex : ADMIN
seulement pour la suppression). Combiner les deux est une pratique
standard : la première ligne de défense filtre au plus tôt (moins cher en
ressources), la seconde exprime des règles précises au bon endroit.

## 6. Pourquoi pas `AuthenticationManager` pour le login ?

`AuthService.login()` vérifie lui-même le mot de passe
(`passwordEncoder.matches(...)`) plutôt que de passer par
`AuthenticationManager`/`AuthenticationProvider`. C'est un choix
pédagogique : le déroulé complet de la vérification reste visible dans une
seule méthode, sans machinerie Spring cachée. Une fois ce mécanisme bien
compris, les étudiants pourront aborder `AuthenticationManager` (utile
pour brancher plusieurs sources d'authentification : base de données,
LDAP, OAuth2...) en comprenant ce qu'il automatise.

## 7. Exercices suggérés

1. Ajouter un endpoint `GET /api/v1/auth/me` qui renvoie les informations
   de l'utilisateur actuellement connecté (indice : injecter
   `Authentication` ou `@AuthenticationPrincipal UserDetails` dans le
   contrôleur).
2. Ajouter un `RefreshTokenService` : un second jeton, de durée de vie plus
   longue, permettant d'obtenir un nouveau jeton d'accès sans redemander
   le mot de passe.
3. Restreindre les opérations `depot`/`retrait`/`virement` au propriétaire
   réel du compte (nécessite de relier `User` et `Client`, actuellement
   indépendants dans ce projet).
4. Ajouter un `@ExceptionHandler(UsernameNotFoundException.class)` et
   vérifier qu'il n'est en réalité jamais atteint depuis `AuthService`
   (pourquoi ? — indice : `AuthService.login()` intercepte déjà ce cas via
   `InvalidCredentialsException`).
