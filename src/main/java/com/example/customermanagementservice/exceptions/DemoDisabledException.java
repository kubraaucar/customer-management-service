package com.example.customermanagementservice.exceptions;

public class DemoDisabledException extends RuntimeException {

    public DemoDisabledException() {
        super("Demo girişi şu anda kapalı.");
    }
}
