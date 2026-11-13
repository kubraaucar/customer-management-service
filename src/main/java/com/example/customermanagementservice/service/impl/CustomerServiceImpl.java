package com.example.customermanagementservice.service.impl;

import com.example.customermanagementservice.controller.CustomerRequest;
import com.example.customermanagementservice.exceptions.CustomerNotFoundException;
import com.example.customermanagementservice.exceptions.DuplicateCitizenNumberException;
import com.example.customermanagementservice.model.Customer;
import com.example.customermanagementservice.repository.CustomerRepository;
import com.example.customermanagementservice.service.CustomerService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public List<Customer> getAll() {
        return customerRepository.findAll();
    }

    @Override
    public List<Customer> getByName(String name) {
        return customerRepository.findByName(name);
    }

    @Override
    public Customer addCustomer(CustomerRequest customerRequestDTO) {
        long citizenNumber = customerRequestDTO.getCitizenNumber();
        if (customerRepository.existsByCitizenNumber(citizenNumber)) {
            throw new DuplicateCitizenNumberException(citizenNumber);
        }

        Customer customer = new Customer();
        customer.setName(customerRequestDTO.getName());
        customer.setSurname(customerRequestDTO.getSurname());
        customer.setCitizenNumber(citizenNumber);
        customer.setBirthDate(customerRequestDTO.getBirthDate());
        customer.setActive(customerRequestDTO.isActive());
        return customerRepository.save(customer);
    }

    @Override
    public void deleteCustomer(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new CustomerNotFoundException("Customer not found: " + customerId);
        }
        customerRepository.deleteById(customerId);
    }

    @Override
    public Customer updateActivation(long citizenNumber) {
        Customer customer = customerRepository.findByCitizenNumber(citizenNumber)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "No customer with citizen number " + citizenNumber));
        customer.setActive(!customer.isActive());
        return customerRepository.save(customer);
    }

}
