package com.example.customermanagementservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Body of POST /api/customers and PUT /api/customers/{id}.
 */
public record CustomerRequest(
        @NotBlank(message = "Ad boş olamaz.")
        @Size(max = 100, message = "Ad en fazla 100 karakter olabilir.")
        String name,

        @NotBlank(message = "Soyad boş olamaz.")
        @Size(max = 100, message = "Soyad en fazla 100 karakter olabilir.")
        String surname,

        @NotNull(message = "TC kimlik numarası boş olamaz.")
        @Min(value = 10_000_000_000L, message = "TC kimlik numarası 11 haneli olmalıdır.")
        @Max(value = 99_999_999_999L, message = "TC kimlik numarası 11 haneli olmalıdır.")
        Long citizenNumber,

        @Past(message = "Doğum tarihi geçmişte bir tarih olmalıdır.")
        LocalDate birthDate,

        // JSON field is "active"; "isActive" is also accepted so it isn't silently ignored. Missing means false.
        @JsonAlias("isActive")
        Boolean active
) {
    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
