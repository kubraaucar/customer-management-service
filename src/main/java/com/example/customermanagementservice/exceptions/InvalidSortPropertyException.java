package com.example.customermanagementservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidSortPropertyException extends RuntimeException {

    public InvalidSortPropertyException(String property) {
        super("Bu alana göre sıralama yapılamaz: " + property);
    }
}
