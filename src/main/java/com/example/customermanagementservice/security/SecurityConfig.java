package com.example.customermanagementservice.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Who may call what, and how a request proves who it comes from.
 * <p>
 * The client logs in at /api/auth/login (or /api/auth/demo) and gets a signed token (JWT).
 * It then sends that token with every request in the header {@code Authorization: Bearer <token>}.
 * No cookies or server sessions are used, so the frontend can live on a different address.
 */
@Configuration
@EnableConfigurationProperties(AppSecurityProperties.class)
public class SecurityConfig {

    /** Name of the token field that holds the user's role, e.g. "roles": ["ADMIN"]. */
    static final String ROLES_CLAIM = "roles";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver)
            throws Exception {
        // 401/403 errors are sent to GeneralExceptionHandler so they get the same Turkish JSON format as other errors.
        AuthenticationEntryPoint notLoggedIn =
                (request, response, exception) -> exceptionResolver.resolveException(request, response, null, exception);
        AccessDeniedHandler notAllowed =
                (request, response, exception) -> exceptionResolver.resolveException(request, response, null, exception);

        http
                // CSRF attacks need cookies; this API only accepts tokens in a header, so CSRF protection is not needed.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/demo").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                        // Reading is allowed for every signed-in user, changing only for administrators.
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("ADMIN", "VIEWER")
                        .requestMatchers("/api/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(notLoggedIn)
                        .accessDeniedHandler(notAllowed))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(notLoggedIn)
                        .accessDeniedHandler(notAllowed));
        return http.build();
    }

    /** Turns "roles": ["ADMIN"] in the token into the Spring authority ROLE_ADMIN. */
    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(ROLES_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    private static SecretKey signingKey(AppSecurityProperties properties) {
        byte[] secret = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(AppSecurityProperties properties) {
        return NimbusJwtEncoder.withSecretKey(signingKey(properties)).build();
    }

    /** Checks the signature and the expiry time of incoming tokens. */
    @Bean
    JwtDecoder jwtDecoder(AppSecurityProperties properties) {
        return NimbusJwtDecoder.withSecretKey(signingKey(properties)).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /** Lets the frontend (on another address, e.g. http://localhost:5173) call /api/**. */
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(allowedOrigins));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        cors.setExposedHeaders(List.of("Location"));
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }
}
