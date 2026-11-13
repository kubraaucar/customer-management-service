package com.example.customermanagementservice.controller;

import com.example.customermanagementservice.dto.ActivationRequest;
import com.example.customermanagementservice.dto.CustomerRequest;
import com.example.customermanagementservice.dto.CustomerResponse;
import com.example.customermanagementservice.service.CustomerService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // GET /api/customers?search=ali&page=0&size=20&sort=name,asc
    @GetMapping
    public PagedModel<CustomerResponse> search(@RequestParam(required = false) String search,
                                               @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return new PagedModel<>(customerService.search(search, pageable));
    }

    @GetMapping("/{id}")
    public CustomerResponse getById(@PathVariable UUID id) {
        return customerService.getById(id);
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse created = customerService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody CustomerRequest request) {
        return customerService.update(id, request);
    }

    // PATCH /api/customers/{id}/activation  body: {"active": true}
    @PatchMapping("/{id}/activation")
    public CustomerResponse setActivation(@PathVariable UUID id, @Valid @RequestBody ActivationRequest request) {
        return customerService.setActive(id, request.active());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
