package com.example.customermanagementservice.security;

import com.example.customermanagementservice.dto.LoginRequest;
import com.example.customermanagementservice.dto.TokenResponse;
import com.example.customermanagementservice.exceptions.DemoDisabledException;
import com.example.customermanagementservice.model.Role;
import com.example.customermanagementservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final TokenService tokenService = mock(TokenService.class);

    private AuthService authService(boolean demoEnabled) {
        AppSecurityProperties properties = new AppSecurityProperties(
                new AppSecurityProperties.Jwt("x".repeat(32), Duration.ofHours(1)),
                new AppSecurityProperties.Admin("admin", "admin12345"),
                new AppSecurityProperties.Demo(demoEnabled, "demo"));
        return new AuthService(authenticationManager, tokenService, properties);
    }

    private static TokenResponse token(String username, Role role) {
        return new TokenResponse("token", Instant.now(), username, role);
    }

    @Test
    void login_issuesTokenWithTheUsersRole() {
        when(authenticationManager.authenticate(any())).thenReturn(UsernamePasswordAuthenticationToken.authenticated(
                "admin", null, AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
        when(tokenService.issue("admin", Role.ADMIN)).thenReturn(token("admin", Role.ADMIN));

        TokenResponse response = authService(true).login(new LoginRequest("admin", "admin12345"));

        assertEquals(Role.ADMIN, response.role());
    }

    @Test
    void login_withWrongPassword_issuesNoToken() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> authService(true).login(new LoginRequest("admin", "wrong")));
        verifyNoInteractions(tokenService);
    }

    @Test
    void demo_issuesReadOnlyToken() {
        when(tokenService.issue("demo", Role.VIEWER)).thenReturn(token("demo", Role.VIEWER));

        assertEquals(Role.VIEWER, authService(true).loginAsDemo().role());
    }

    @Test
    void demo_whenDisabled_throws() {
        assertThrows(DemoDisabledException.class, () -> authService(false).loginAsDemo());
        verifyNoInteractions(tokenService);
    }
}
