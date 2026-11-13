package com.example.customermanagementservice.controller;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Date;


public class CustomerRequest {
    @NotBlank
    private String name;
    private String surname;
    @NotNull
    @Min(value = 10_000_000_000L, message = "must be an 11-digit number")
    @Max(value = 99_999_999_999L, message = "must be an 11-digit number")
    private Long citizenNumber;
    private Date birthDate;
    // JSON field is "active"; "isActive" is also accepted so it isn't silently ignored.
    @JsonAlias("isActive")
    private boolean active;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public Long getCitizenNumber() {
        return citizenNumber;
    }

    public void setCitizenNumber(Long citizenNumber) {
        this.citizenNumber = citizenNumber;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
