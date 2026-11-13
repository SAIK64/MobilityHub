package projet.saik.config;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
// Configure l’authentification web par session et l’authentification API par jeton JWT.
public class SecurityConfig {
    // Journalise les cas où un jeton de remplacement est généré sans secret configuré.
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    // Fournit l’encodeur BCrypt utilisé à l’inscription et à la vérification des mots de passe.
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    // Expose le gestionnaire d’authentification Spring aux contrôleurs de connexion.
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    // Construit la clé HMAC utilisée pour signer et vérifier les JWT.
    SecretKey jwtSecret(@Value("${app.jwt.secret:}") String secret) {
        byte[] bytes;
        if (secret.isBlank()) {
            // Génère une clé temporaire ; les jetons ne survivront pas au redémarrage.
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            log.warn("APP_JWT_SECRET absent : les jetons API seront invalidés au redémarrage.");
        } else {
            // Utilise le secret de configuration et impose une taille minimale de sécurité.
            bytes = secret.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("APP_JWT_SECRET doit contenir au moins 32 octets.");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    // Fournit le composant qui signe les jetons émis par l’API.
    JwtEncoder jwtEncoder(SecretKey secret) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secret));
    }

    @Bean
    // Fournit le composant qui vérifie la signature et l’émetteur des jetons reçus.
    JwtDecoder jwtDecoder(SecretKey secret) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secret).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("saik"));
        return decoder;
    }

    @Bean
    @Order(1)
    // API REST : les routes d’authentification acceptent les connexions sans jeton.
    SecurityFilterChain tokenApi(HttpSecurity http) throws Exception {
        // Convertit les rôles déclarés dans le JWT en autorisations Spring.
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        RequestMatcher tokenApi = request -> path(request).startsWith("/api/v1/auth/")
                || (path(request).startsWith("/api/")
                    && request.getHeader("Authorization") != null
                    && request.getHeader("Authorization").startsWith("Bearer "));
        // Rend l’API sans état et exige un jeton valide pour ses routes protégées.
        http.securityMatcher(tokenApi)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/register").permitAll()
                        .requestMatchers("/api/v1/students", "/api/v1/mobilities/pending").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));
        return http.build();
    }

    @Bean
    // Protège les pages HTML par session et le formulaire de connexion Spring Security.
    SecurityFilterChain web(HttpSecurity http) throws Exception {
        // Définit l’accès aux pages publiques, à la console H2 et aux routes d’administration.
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/accueil", "/login", "/register", "/error", "/js/**", "/css/**", "/images/**").permitAll()
                .requestMatchers("/h2-console/**").denyAll()
                .requestMatchers("/admin/**", "/api/v1/students", "/api/v1/mobilities/pending").hasRole("ADMIN")
                .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor(
                        new org.springframework.security.web.authentication.HttpStatusEntryPoint(
                                org.springframework.http.HttpStatus.UNAUTHORIZED),
                        request -> path(request).startsWith("/api/"))
                        .defaultAuthenticationEntryPointFor(
                                new org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint("/login"),
                                request -> !path(request).startsWith("/api/")));
        return http.build();
    }

    private static String path(jakarta.servlet.http.HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
