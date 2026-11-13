package com.example.customermanagementservice;

import com.example.customermanagementservice.model.Customer;
import com.example.customermanagementservice.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.URI;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Runs the whole app against a real PostgreSQL started in Docker (Flyway migration + JPA + HTTP).
 * Skipped automatically when Docker is not running.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class CustomerApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void cleanDatabase() {
        customerRepository.deleteAll();
    }

    private void save(String name, String surname, long citizenNumber) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setSurname(surname);
        customer.setCitizenNumber(citizenNumber);
        customer.setActive(true);
        customerRepository.save(customer);
    }

    @Test
    void customerLifecycle_createReadUpdateActivateDelete() throws Exception {
        String location = mockMvc.perform(post("/api/customers").contentType(APPLICATION_JSON).content("""
                        {"name": "Kübra", "surname": "Uçar", "citizenNumber": 12345678901, "birthDate": "2002-01-08", "active": true}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.birthDate").value("2002-01-08"))
                .andReturn().getResponse().getHeader("Location");
        URI customerUri = URI.create(location);

        // Same TC number again -> 409
        mockMvc.perform(post("/api/customers").contentType(APPLICATION_JSON).content("""
                        {"name": "Başka", "surname": "Kişi", "citizenNumber": 12345678901}
                        """))
                .andExpect(status().isConflict());

        mockMvc.perform(get(customerUri))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kübra"));

        mockMvc.perform(put(customerUri).contentType(APPLICATION_JSON).content("""
                        {"name": "Kübra", "surname": "Yılmaz", "citizenNumber": 12345678901, "birthDate": "2002-01-08", "active": true}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.surname").value("Yılmaz"));

        mockMvc.perform(patch(customerUri + "/activation").contentType(APPLICATION_JSON).content("""
                        {"active": false}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete(customerUri)).andExpect(status().isNoContent());
        mockMvc.perform(get(customerUri)).andExpect(status().isNotFound());
    }

    @Test
    void search_isCaseInsensitive_matchesFullName_andPages() throws Exception {
        save("Ali", "Yılmaz", 11111111111L);
        save("Ayşe", "Demir", 22222222222L);
        save("Ali", "Kaya", 33333333333L);

        mockMvc.perform(get("/api/customers").param("search", "ALI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2));

        mockMvc.perform(get("/api/customers").param("search", "ali kaya"))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].surname").value("Kaya"));

        mockMvc.perform(get("/api/customers").param("size", "2").param("sort", "surname,asc"))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].surname").value("Demir"))
                .andExpect(jsonPath("$.page.totalPages").value(2));

        mockMvc.perform(get("/api/customers").param("sort", "password"))
                .andExpect(status().isBadRequest());
    }
}
