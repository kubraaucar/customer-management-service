package com.example.customermanagementservice;

import com.example.customermanagementservice.controller.CustomerController;
import com.example.customermanagementservice.controller.CustomerRequest;
import com.example.customermanagementservice.exceptions.CustomerNotFoundException;
import com.example.customermanagementservice.exceptions.DuplicateCitizenNumberException;
import com.example.customermanagementservice.model.Customer;
import com.example.customermanagementservice.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests the HTTP layer: URLs, status codes and JSON bodies (the service is mocked).
 */
@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    private static final long CITIZEN_NUMBER = 12345678901L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    private static Customer customer(String name, boolean active) {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setName(name);
        customer.setCitizenNumber(CITIZEN_NUMBER);
        customer.setActive(active);
        return customer;
    }

    @Test
    void getAll_returns200WithCustomers() throws Exception {
        when(customerService.getAll()).thenReturn(List.of(customer("kubra", true)));

        mockMvc.perform(get("/customer/getAll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("kubra"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void getByName_returns200WithMatches() throws Exception {
        when(customerService.getByName("kubra")).thenReturn(List.of(customer("kubra", true)));

        mockMvc.perform(get("/customer/getByName/kubra"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].citizenNumber").value(CITIZEN_NUMBER));
    }

    @Test
    void add_withValidBody_returns201() throws Exception {
        when(customerService.addCustomer(any(CustomerRequest.class))).thenReturn(customer("kubra", true));

        mockMvc.perform(post("/customer/add").contentType(APPLICATION_JSON).content("""
                        {"name": "kubra", "surname": "ucar", "citizenNumber": 12345678901, "active": true}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("kubra"));
    }

    @Test
    void add_acceptsIsActiveAsFieldName() throws Exception {
        when(customerService.addCustomer(any(CustomerRequest.class))).thenReturn(customer("kubra", true));

        mockMvc.perform(post("/customer/add").contentType(APPLICATION_JSON).content("""
                        {"name": "kubra", "citizenNumber": 12345678901, "isActive": true}
                        """))
                .andExpect(status().isCreated());

        ArgumentCaptor<CustomerRequest> captor = ArgumentCaptor.forClass(CustomerRequest.class);
        verify(customerService).addCustomer(captor.capture());
        assertTrue(captor.getValue().isActive());
    }

    @Test
    void add_withBlankName_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/customer/add").contentType(APPLICATION_JSON).content("""
                        {"name": "", "citizenNumber": 12345678901}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        verify(customerService, never()).addCustomer(any());
    }

    @Test
    void add_withMissingOrShortCitizenNumber_returns400() throws Exception {
        mockMvc.perform(post("/customer/add").contentType(APPLICATION_JSON).content("""
                        {"name": "kubra"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.citizenNumber").exists());

        mockMvc.perform(post("/customer/add").contentType(APPLICATION_JSON).content("""
                        {"name": "kubra", "citizenNumber": 1234}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.citizenNumber").value("must be an 11-digit number"));

        verify(customerService, never()).addCustomer(any());
    }

    @Test
    void add_withMalformedJson_returns400() throws Exception {
        mockMvc.perform(post("/customer/add").contentType(APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void add_withDuplicateCitizenNumber_returns409() throws Exception {
        when(customerService.addCustomer(any(CustomerRequest.class)))
                .thenThrow(new DuplicateCitizenNumberException(CITIZEN_NUMBER));

        mockMvc.perform(post("/customer/add").contentType(APPLICATION_JSON).content("""
                        {"name": "kubra", "citizenNumber": 12345678901}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("already exists")));
    }

    @Test
    void remove_existingCustomer_returns204() throws Exception {
        UUID customerId = UUID.randomUUID();

        mockMvc.perform(delete("/customer/remove/{id}", customerId))
                .andExpect(status().isNoContent());

        verify(customerService).deleteCustomer(customerId);
    }

    @Test
    void remove_missingCustomer_returns404() throws Exception {
        UUID customerId = UUID.randomUUID();
        doThrow(new CustomerNotFoundException("Customer not found: " + customerId))
                .when(customerService).deleteCustomer(customerId);

        mockMvc.perform(delete("/customer/remove/{id}", customerId))
                .andExpect(status().isNotFound());
    }

    @Test
    void remove_withInvalidId_returns400() throws Exception {
        mockMvc.perform(delete("/customer/remove/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateActivation_existingCustomer_returns200() throws Exception {
        when(customerService.updateActivation(CITIZEN_NUMBER)).thenReturn(customer("kubra", false));

        mockMvc.perform(put("/customer/update/{citizenNumber}", CITIZEN_NUMBER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void updateActivation_missingCustomer_returns404() throws Exception {
        when(customerService.updateActivation(CITIZEN_NUMBER))
                .thenThrow(new CustomerNotFoundException("No customer with citizen number " + CITIZEN_NUMBER));

        mockMvc.perform(put("/customer/update/{citizenNumber}", CITIZEN_NUMBER))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownUrl_returns404() throws Exception {
        mockMvc.perform(get("/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unexpectedError_returns500WithoutInternalDetails() throws Exception {
        when(customerService.getAll()).thenThrow(new IllegalStateException("secret database detail"));

        mockMvc.perform(get("/customer/getAll"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
                .andExpect(content().string(not(containsString("secret"))));
    }
}
