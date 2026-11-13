package com.example.customermanagementservice.model;

/** What a signed-in user may do. */
public enum Role {
    /** Can read and change customers. */
    ADMIN,
    /** Can only read customers (used by the demo login). */
    VIEWER
}
