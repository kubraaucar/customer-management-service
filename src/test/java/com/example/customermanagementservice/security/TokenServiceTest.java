package com.example.customermanagementservice.security;

import com.example.customermanagementservice.dto.TokenResponse;
import com.example.customermanagementservice.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenServiceTest {

    private static AppSecurityProperties properties(String secret) {
        return new AppSecurityProperties(
                new AppSecurityProperties.Jwt(secret, Duration.ofHours(2)),
                new AppSecurityProperties.Admin("admin", "admin12345"),
                new AppSecurityProperties.Demo(true, "demo"));
    }

    private final AppSecurityProperties properties = properties("test-secret-test-secret-test-secret-123");
    private final SecurityConfig config = new SecurityConfig();
    private final TokenService tokenService = new TokenService(config.jwtEncoder(properties), properties);
    private final JwtDecoder decoder = config.jwtDecoder(properties);

    @Test
    void issuedToken_containsUserRoleAndExpiry_andIsAcceptedByTheDecoder() {
        Instant before = Instant.now();

        TokenResponse response = tokenService.issue("admin", Role.ADMIN);
        Jwt jwt = decoder.decode(response.token());

        assertEquals("admin", jwt.getSubject());
        assertEquals(List.of("ADMIN"), jwt.getClaimAsStringList(SecurityConfig.ROLES_CLAIM));
        assertEquals(response.expiresAt(), jwt.getExpiresAt());
        assertFalse(response.expiresAt().isBefore(before.plus(Duration.ofHours(2)).minusSeconds(1)));
        assertEquals(Role.ADMIN, response.role());
    }

    @Test
    void tokenSignedWithAnotherSecret_isRejected() {
        AppSecurityProperties other = properties("another-secret-another-secret-another-1");
        String foreignToken = new TokenService(config.jwtEncoder(other), other).issue("admin", Role.ADMIN).token();

        assertThrows(JwtException.class, () -> decoder.decode(foreignToken));
    }
}
