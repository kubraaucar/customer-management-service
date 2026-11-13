package com.example.customermanagementservice;

import com.example.customermanagementservice.controller.CustomerRequest;
import com.example.customermanagementservice.exceptions.CustomerNotFoundException;
import com.example.customermanagementservice.exceptions.DuplicateCitizenNumberException;
import com.example.customermanagementservice.model.Customer;
import com.example.customermanagementservice.repository.CustomerRepository;
import com.example.customermanagementservice.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    private static final long CITIZEN_NUMBER = 12345678901L;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Test
    void getAll_when_called_then_returnSuccess() {
        Customer expected = new Customer();
        expected.setCitizenNumber(CITIZEN_NUMBER);
        when(customerRepository.findAll()).thenReturn(List.of(expected));

        List<Customer> actual = customerService.getAll();

        assertEquals(List.of(expected), actual);
    }

    @Test
    void getByName_when_validNameProvided_then_returnSuccess() {
        Customer expected = new Customer();
        expected.setName("kubra");
        when(customerRepository.findByName("kubra")).thenReturn(List.of(expected));

        List<Customer> actual = customerService.getByName("kubra");

        assertEquals(List.of(expected), actual);
    }

    @Test
    void addCustomer_when_validCustomerRequestProvided_then_Success() {
        CustomerRequest request = new CustomerRequest();
        request.setName("kubra");
        request.setSurname("ucar");
        request.setCitizenNumber(CITIZEN_NUMBER);
        request.setActive(true);
        when(customerRepository.existsByCitizenNumber(CITIZEN_NUMBER)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer actual = customerService.addCustomer(request);

        assertEquals("kubra", actual.getName());
        assertEquals("ucar", actual.getSurname());
        assertEquals(CITIZEN_NUMBER, actual.getCitizenNumber());
        assertTrue(actual.isActive());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void addCustomer_when_citizenNumberAlreadyExists_then_throwDuplicate() {
        CustomerRequest request = new CustomerRequest();
        request.setName("kubra");
        request.setCitizenNumber(CITIZEN_NUMBER);
        when(customerRepository.existsByCitizenNumber(CITIZEN_NUMBER)).thenReturn(true);

        assertThrows(DuplicateCitizenNumberException.class, () -> customerService.addCustomer(request));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void deleteCustomer_when_customerExists_then_Success() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.existsById(customerId)).thenReturn(true);

        customerService.deleteCustomer(customerId);

        verify(customerRepository).deleteById(customerId);
    }

    @Test
    void deleteCustomer_when_customerMissing_then_throwNotFound() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.existsById(customerId)).thenReturn(false);

        assertThrows(CustomerNotFoundException.class, () -> customerService.deleteCustomer(customerId));
        verify(customerRepository, never()).deleteById(any());
    }

    @Test
    void updateActivation_when_validCitizenNumberProvided_then_toggleActive() {
        Customer customer = new Customer();
        customer.setCitizenNumber(CITIZEN_NUMBER);
        customer.setActive(true);
        when(customerRepository.findByCitizenNumber(CITIZEN_NUMBER)).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);

        Customer updated = customerService.updateActivation(CITIZEN_NUMBER);

        assertFalse(updated.isActive());
        verify(customerRepository).save(customer);
    }

    @Test
    void updateActivation_when_customerMissing_then_throwNotFound() {
        when(customerRepository.findByCitizenNumber(CITIZEN_NUMBER)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> customerService.updateActivation(CITIZEN_NUMBER));
        verify(customerRepository, never()).save(any());
    }
}
