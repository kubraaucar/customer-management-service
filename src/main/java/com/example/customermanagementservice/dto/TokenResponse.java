package com.example.customermanagementservice.dto;

import com.example.customermanagementservice.model.Role;

import java.time.Instant;

/**
 * Answer to a successful login.
 *
 * @param token     send it back as "Authorization: Bearer &lt;token&gt;"
 * @param expiresAt after this moment the token is no longer accepted
 */
public record TokenResponse(String token, Instant expiresAt, String username, Role role) {
}
