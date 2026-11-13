package projet.saik.controller;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import projet.saik.service.AccountService;
import projet.saik.web.AuthRequest;

@RestController
@RequestMapping("/api/v1/auth")
// Expose les points d’entrée JSON de création de compte et de connexion JWT.
public class AuthApiController {
    // Service qui crée les comptes avec leurs règles de validation.
    private final AccountService accounts;
    // Vérifie les identifiants avant d’émettre un jeton.
    private final AuthenticationManager authenticationManager;
    // Signe les jetons d’accès remis au client.
    private final JwtEncoder jwtEncoder;
    // Durée de validité du jeton exprimée en secondes.
    private final long expirationSeconds;

    // Reçoit les dépendances Spring et convertit la durée configurée en secondes.
    public AuthApiController(AccountService accounts, AuthenticationManager authenticationManager, JwtEncoder jwtEncoder,
            @Value("${app.jwt.expiration-minutes:60}") long expirationMinutes) {
        this.accounts = accounts;
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        // Refuse une configuration qui produirait un jeton déjà expiré ou un dépassement numérique.
        if (expirationMinutes <= 0 || expirationMinutes > Long.MAX_VALUE / 60) {
            throw new IllegalArgumentException("La durée de validité JWT doit être positive.");
        }
        this.expirationSeconds = expirationMinutes * 60;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    // Crée un compte et renvoie ses informations publiques sans renvoyer son mot de passe.
    public RegisteredUser register(@Valid @RequestBody AuthRequest request) {
        var user = accounts.register(request.getEmail(), request.getPassword());
        return new RegisteredUser(user.getId(), user.getEmail(), "USER");
    }

    @PostMapping("/login")
    // Authentifie les identifiants et renvoie un jeton Bearer signé.
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            // Demande à Spring Security de vérifier l’e-mail et le mot de passe.
            var auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
            Instant now = Instant.now();
            // Convertit les autorisations en noms de rôles à inclure dans les claims du jeton.
            List<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                    .map(role -> role.replaceFirst("^ROLE_", "")).toList();
            // Définit l’émetteur, l’utilisateur, les dates de validité et les rôles du jeton.
            var claims = JwtClaimsSet.builder().issuer("saik").subject(auth.getName())
                    .issuedAt(now).expiresAt(now.plusSeconds(expirationSeconds)).claim("roles", roles).build();
            // Choisit l’algorithme HMAC configuré pour signer le JWT.
            var header = JwsHeader.with(MacAlgorithm.HS256).build();
            String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
            return new TokenResponse(token, "Bearer", expirationSeconds);
        } catch (AuthenticationException ex) {
            // Ne révèle pas si l’adresse ou le mot de passe était incorrect.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou mot de passe incorrect.", ex);
        }
    }

    // Champs obligatoires reçus lors d’une demande de connexion.
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    // Données non sensibles renvoyées après la création d’un compte.
    public record RegisteredUser(Long id, String email, String role) {}
    // Format JSON du jeton renvoyé après une connexion réussie.
    public record TokenResponse(String token, String type, long expiresInSeconds) {}
}
