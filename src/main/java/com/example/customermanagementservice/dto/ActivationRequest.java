package com.example.customermanagementservice.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Body of PATCH /api/customers/{id}/activation, e.g. {"active": false}.
 */
public record ActivationRequest(
        @NotNull(message = "Aktiflik durumu (active) boş olamaz.")
        Boolean active
) {
}
