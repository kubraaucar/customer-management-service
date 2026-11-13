package com.example.customermanagementservice.service;

import com.example.customermanagementservice.dto.LoginRequest;
import com.example.customermanagementservice.dto.TokenResponse;
import com.example.customermanagementservice.exceptions.DemoDisabledException;
import com.example.customermanagementservice.model.Role;
import com.example.customermanagementservice.security.AppSecurityProperties;
import com.example.customermanagementservice.security.TokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String ROLE_PREFIX = "ROLE_";

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final AppSecurityProperties.Demo demo;

    public AuthService(AuthenticationManager authenticationManager,
                       TokenService tokenService,
                       AppSecurityProperties properties) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.demo = properties.demo();
    }

    /**
     * Checks the username and password and returns a token.
     *
     * @throws org.springframework.security.authentication.BadCredentialsException when they are wrong
     */
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        return tokenService.issue(authentication.getName(), roleOf(authentication));
    }

    /** Returns a read-only token for the demo user, without a password. */
    public TokenResponse loginAsDemo() {
        if (!demo.enabled()) {
            throw new DemoDisabledException();
        }
        return tokenService.issue(demo.username(), Role.VIEWER);
    }

    private static Role roleOf(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority != null && authority.startsWith(ROLE_PREFIX))
                .map(authority -> Role.valueOf(authority.substring(ROLE_PREFIX.length())))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User has no role: " + authentication.getName()));
    }
}
