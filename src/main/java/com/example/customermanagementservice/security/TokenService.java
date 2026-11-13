package com.example.customermanagementservice.security;

import com.example.customermanagementservice.dto.TokenResponse;
import com.example.customermanagementservice.model.Role;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Creates the signed login tokens (JWT). */
@Service
public class TokenService {

    static final String ISSUER = "customer-management-service";

    private final JwtEncoder jwtEncoder;
    private final Duration expiration;

    public TokenService(JwtEncoder jwtEncoder, AppSecurityProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.expiration = properties.jwt().expiration();
    }

    public TokenResponse issue(String username, Role role) {
        // Tokens store times in whole seconds; cut the rest so expiresAt in the answer matches the token.
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = now.plus(expiration);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(username)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(SecurityConfig.ROLES_CLAIM, List.of(role.name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, expiresAt, username, role);
    }
}
