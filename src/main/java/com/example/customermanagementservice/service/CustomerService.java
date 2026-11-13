package com.example.customermanagementservice.service;

import com.example.customermanagementservice.dto.CustomerRequest;
import com.example.customermanagementservice.dto.CustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomerService {

    /** Lists customers page by page; {@code search} (optional) matches name/surname, case-insensitive. */
    Page<CustomerResponse> search(String search, Pageable pageable);

    CustomerResponse getById(UUID id);

    CustomerResponse create(CustomerRequest request);

    CustomerResponse update(UUID id, CustomerRequest request);

    CustomerResponse setActive(UUID id, boolean active);

    void delete(UUID id);
}
