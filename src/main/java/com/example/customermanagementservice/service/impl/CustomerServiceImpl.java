package com.example.customermanagementservice.service.impl;

import com.example.customermanagementservice.dto.CustomerRequest;
import com.example.customermanagementservice.dto.CustomerResponse;
import com.example.customermanagementservice.exceptions.CustomerNotFoundException;
import com.example.customermanagementservice.exceptions.DuplicateCitizenNumberException;
import com.example.customermanagementservice.exceptions.InvalidSortPropertyException;
import com.example.customermanagementservice.model.Customer;
import com.example.customermanagementservice.repository.CustomerRepository;
import com.example.customermanagementservice.service.CustomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("name", "surname", "citizenNumber", "birthDate", "active");

    private final CustomerRepository customerRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Page<CustomerResponse> search(String search, Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new InvalidSortPropertyException(order.getProperty());
            }
        }

        Page<Customer> page = (search == null || search.isBlank())
                ? customerRepository.findAll(pageable)
                : customerRepository.search(search.trim(), pageable);
        return page.map(CustomerResponse::from);
    }

    @Override
    public CustomerResponse getById(UUID id) {
        return CustomerResponse.from(findOrThrow(id));
    }

    @Override
    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        if (customerRepository.existsByCitizenNumber(request.citizenNumber())) {
            throw new DuplicateCitizenNumberException(request.citizenNumber());
        }

        Customer customer = new Customer();
        copyFields(request, customer);
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public CustomerResponse update(UUID id, CustomerRequest request) {
        Customer customer = findOrThrow(id);
        boolean citizenNumberChanged = customer.getCitizenNumber() != request.citizenNumber();
        if (citizenNumberChanged && customerRepository.existsByCitizenNumber(request.citizenNumber())) {
            throw new DuplicateCitizenNumberException(request.citizenNumber());
        }

        copyFields(request, customer);
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public CustomerResponse setActive(UUID id, boolean active) {
        Customer customer = findOrThrow(id);
        customer.setActive(active);
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!customerRepository.existsById(id)) {
            throw new CustomerNotFoundException(id);
        }
        customerRepository.deleteById(id);
    }

    private Customer findOrThrow(UUID id) {
        return customerRepository.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
    }

    private static void copyFields(CustomerRequest request, Customer customer) {
        customer.setName(request.name().trim());
        customer.setSurname(request.surname().trim());
        customer.setCitizenNumber(request.citizenNumber());
        customer.setBirthDate(request.birthDate());
        customer.setActive(request.isActive());
    }
}
