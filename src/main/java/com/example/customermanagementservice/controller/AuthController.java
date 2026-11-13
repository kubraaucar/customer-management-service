package com.example.customermanagementservice.controller;

import com.example.customermanagementservice.dto.LoginRequest;
import com.example.customermanagementservice.dto.TokenResponse;
import com.example.customermanagementservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@SecurityRequirements // these endpoints work without a token
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Log in with username and password")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Log in as the read-only demo user (no password)")
    @PostMapping("/demo")
    public TokenResponse demo() {
        return authService.loginAsDemo();
    }
}
