package com.example.customermanagementservice;

import com.example.customermanagementservice.dto.CustomerRequest;
import com.example.customermanagementservice.dto.CustomerResponse;
import com.example.customermanagementservice.exceptions.CustomerNotFoundException;
import com.example.customermanagementservice.exceptions.DuplicateCitizenNumberException;
import com.example.customermanagementservice.exceptions.InvalidSortPropertyException;
import com.example.customermanagementservice.model.Customer;
import com.example.customermanagementservice.repository.CustomerRepository;
import com.example.customermanagementservice.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    private static final long CITIZEN_NUMBER = 12345678901L;
    private static final long OTHER_CITIZEN_NUMBER = 10987654321L;
    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20, Sort.by("name"));

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private static Customer customer(UUID id, long citizenNumber, boolean active) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName("Kubra");
        customer.setSurname("Ucar");
        customer.setCitizenNumber(citizenNumber);
        customer.setActive(active);
        return customer;
    }

    private static CustomerRequest request(String name, String surname, long citizenNumber, boolean active) {
        return new CustomerRequest(name, surname, citizenNumber, LocalDate.of(2002, 1, 8), active);
    }

    @Test
    void search_withoutText_returnsAllCustomersPaged() {
        Customer customer = customer(UUID.randomUUID(), CITIZEN_NUMBER, true);
        when(customerRepository.findAll(FIRST_PAGE)).thenReturn(new PageImpl<>(List.of(customer), FIRST_PAGE, 1));

        Page<CustomerResponse> page = customerService.search("  ", FIRST_PAGE);

        assertEquals(1, page.getTotalElements());
        assertEquals(CustomerResponse.from(customer), page.getContent().get(0));
        verify(customerRepository, never()).search(any(), any());
    }

    @Test
    void search_withText_usesTrimmedSearchQuery() {
        when(customerRepository.search("kubra", FIRST_PAGE)).thenReturn(Page.empty(FIRST_PAGE));

        customerService.search("  kubra ", FIRST_PAGE);

        verify(customerRepository).search("kubra", FIRST_PAGE);
    }

    @Test
    void search_withUnknownSortField_throwsBadRequest() {
        Pageable sortedByPassword = PageRequest.of(0, 20, Sort.by("password"));

        assertThrows(InvalidSortPropertyException.class, () -> customerService.search(null, sortedByPassword));
        verifyNoInteractions(customerRepository);
    }

    @Test
    void getById_whenMissing_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> customerService.getById(id));
    }

    @Test
    void create_withNewCitizenNumber_savesTrimmedFields() {
        when(customerRepository.existsByCitizenNumber(CITIZEN_NUMBER)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse created = customerService.create(request(" Kubra ", "Ucar ", CITIZEN_NUMBER, true));

        assertEquals("Kubra", created.name());
        assertEquals("Ucar", created.surname());
        assertEquals(CITIZEN_NUMBER, created.citizenNumber());
        assertEquals(LocalDate.of(2002, 1, 8), created.birthDate());
        assertTrue(created.active());
    }

    @Test
    void create_withExistingCitizenNumber_throwsDuplicate() {
        when(customerRepository.existsByCitizenNumber(CITIZEN_NUMBER)).thenReturn(true);

        assertThrows(DuplicateCitizenNumberException.class,
                () -> customerService.create(request("Kubra", "Ucar", CITIZEN_NUMBER, true)));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void update_withSameCitizenNumber_savesWithoutDuplicateCheck() {
        UUID id = UUID.randomUUID();
        Customer existing = customer(id, CITIZEN_NUMBER, true);
        when(customerRepository.findById(id)).thenReturn(Optional.of(existing));
        when(customerRepository.save(existing)).thenReturn(existing);

        CustomerResponse updated = customerService.update(id, request("Ayse", "Demir", CITIZEN_NUMBER, false));

        assertEquals("Ayse", updated.name());
        assertEquals("Demir", updated.surname());
        assertFalse(updated.active());
        verify(customerRepository, never()).existsByCitizenNumber(anyLong());
    }

    @Test
    void update_toCitizenNumberOfAnotherCustomer_throwsDuplicate() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer(id, CITIZEN_NUMBER, true)));
        when(customerRepository.existsByCitizenNumber(OTHER_CITIZEN_NUMBER)).thenReturn(true);

        assertThrows(DuplicateCitizenNumberException.class,
                () -> customerService.update(id, request("Kubra", "Ucar", OTHER_CITIZEN_NUMBER, true)));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void update_whenMissing_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class,
                () -> customerService.update(id, request("Kubra", "Ucar", CITIZEN_NUMBER, true)));
    }

    @Test
    void setActive_setsTheGivenValue() {
        UUID id = UUID.randomUUID();
        Customer existing = customer(id, CITIZEN_NUMBER, true);
        when(customerRepository.findById(id)).thenReturn(Optional.of(existing));
        when(customerRepository.save(existing)).thenReturn(existing);

        CustomerResponse result = customerService.setActive(id, false);

        assertFalse(result.active());
    }

    @Test
    void delete_whenExists_deletes() {
        UUID id = UUID.randomUUID();
        when(customerRepository.existsById(id)).thenReturn(true);

        customerService.delete(id);

        verify(customerRepository).deleteById(id);
    }

    @Test
    void delete_whenMissing_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(customerRepository.existsById(id)).thenReturn(false);

        assertThrows(CustomerNotFoundException.class, () -> customerService.delete(id));
        verify(customerRepository, never()).deleteById(any());
    }
}
