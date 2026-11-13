package com.example.customermanagementservice.dto;

import com.example.customermanagementservice.model.Customer;

import java.time.LocalDate;
import java.util.UUID;

/**
 * What the API returns for a customer. Keeps the database entity out of the JSON.
 */
public record CustomerResponse(
        UUID id,
        String name,
        String surname,
        long citizenNumber,
        LocalDate birthDate,
        boolean active
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getSurname(),
                customer.getCitizenNumber(),
                customer.getBirthDate(),
                customer.isActive());
    }
}
