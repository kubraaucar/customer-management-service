package com.example.customermanagementservice.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Kullanıcı adı boş olamaz.") String username,
        @NotBlank(message = "Şifre boş olamaz.") String password) {

    /** Never print the password (e.g. in logs). */
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=***]";
    }
}
