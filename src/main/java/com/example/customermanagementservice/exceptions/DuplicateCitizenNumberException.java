package com.example.customermanagementservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateCitizenNumberException extends RuntimeException {

    public DuplicateCitizenNumberException(long citizenNumber) {
        super(citizenNumber + " TC kimlik numarasıyla kayıtlı bir müşteri zaten var.");
    }
}
