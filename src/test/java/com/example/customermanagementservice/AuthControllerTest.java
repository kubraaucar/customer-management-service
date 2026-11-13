package com.example.customermanagementservice;

import com.example.customermanagementservice.controller.AuthController;
import com.example.customermanagementservice.dto.LoginRequest;
import com.example.customermanagementservice.dto.TokenResponse;
import com.example.customermanagementservice.exceptions.DemoDisabledException;
import com.example.customermanagementservice.model.Role;
import com.example.customermanagementservice.security.SecurityConfig;
import com.example.customermanagementservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The login endpoints work without a token and return Turkish errors. */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final Instant EXPIRES_AT = Instant.parse("2030-01-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void login_withCorrectPassword_returnsToken() throws Exception {
        when(authService.login(new LoginRequest("admin", "secret123")))
                .thenReturn(new TokenResponse("abc.def.ghi", EXPIRES_AT, "admin", Role.ADMIN));

        mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content("""
                        {"username": "admin", "password": "secret123"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("abc.def.ghi"))
                .andExpect(jsonPath("$.expiresAt").value("2030-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void login_withWrongPassword_returns401WithTurkishMessage() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content("""
                        {"username": "admin", "password": "wrong"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Kullanıcı adı veya şifre hatalı."));
    }

    @Test
    void login_withEmptyFields_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content("""
                        {"username": "", "password": " "}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").value("Kullanıcı adı boş olamaz."))
                .andExpect(jsonPath("$.errors.password").value("Şifre boş olamaz."));

        verify(authService, never()).login(any());
    }

    @Test
    void demo_returnsReadOnlyToken() throws Exception {
        when(authService.loginAsDemo()).thenReturn(new TokenResponse("demo.token", EXPIRES_AT, "demo", Role.VIEWER));

        mockMvc.perform(post("/api/auth/demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("demo"))
                .andExpect(jsonPath("$.role").value("VIEWER"));
    }

    @Test
    void demo_whenDisabled_returns404() throws Exception {
        when(authService.loginAsDemo()).thenThrow(new DemoDisabledException());

        mockMvc.perform(post("/api/auth/demo"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Demo girişi şu anda kapalı."));
    }
}
