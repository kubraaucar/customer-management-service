package com.example.customermanagementservice.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * The app.security.* settings from application.properties. The app does not start if they are invalid.
 */
@Validated
@ConfigurationProperties("app.security")
public record AppSecurityProperties(@Valid @NotNull Jwt jwt, @Valid @NotNull Admin admin, @Valid @NotNull Demo demo) {

    /**
     * @param secret     key that signs the login tokens; anyone who knows it can create tokens
     * @param expiration how long a login stays valid
     */
    public record Jwt(@NotBlank @Size(min = 32, message = "must be at least 32 characters") String secret,
                      @NotNull Duration expiration) {
    }

    /** The administrator account that is created or updated at startup. */
    public record Admin(@NotBlank @Size(max = 50) String username,
                        @NotBlank @Size(min = 8, message = "must be at least 8 characters") String password) {
    }

    /** The "Demo olarak dene" login: read-only access without a password. */
    public record Demo(boolean enabled, @NotBlank @Size(max = 50) String username) {
    }
}
