package com.example.customermanagementservice;

import com.example.customermanagementservice.model.Customer;
import com.example.customermanagementservice.repository.CustomerRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.URI;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Runs the whole app against a real PostgreSQL started in Docker (Flyway migration + JPA + security + HTTP).
 * Requests log in for real: the admin account is created at startup from application.properties.
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

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        customerRepository.deleteAll();
        adminToken = tokenFrom(post("/api/auth/login").contentType(APPLICATION_JSON).content("""
                {"username": "admin", "password": "admin12345"}
                """));
    }

    private String tokenFrom(MockHttpServletRequestBuilder loginRequest) throws Exception {
        String body = mockMvc.perform(loginRequest)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    /** Adds the login token to a request, like the frontend does. */
    private static MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + token);
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
        String location = mockMvc.perform(as(adminToken, post("/api/customers")).contentType(APPLICATION_JSON).content("""
                        {"name": "Kübra", "surname": "Uçar", "citizenNumber": 12345678901, "birthDate": "2002-01-08", "active": true}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.birthDate").value("2002-01-08"))
                .andReturn().getResponse().getHeader("Location");
        URI customerUri = URI.create(location);

        // Same TC number again -> 409
        mockMvc.perform(as(adminToken, post("/api/customers")).contentType(APPLICATION_JSON).content("""
                        {"name": "Başka", "surname": "Kişi", "citizenNumber": 12345678901}
                        """))
                .andExpect(status().isConflict());

        mockMvc.perform(as(adminToken, get(customerUri)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kübra"));

        mockMvc.perform(as(adminToken, put(customerUri)).contentType(APPLICATION_JSON).content("""
                        {"name": "Kübra", "surname": "Yılmaz", "citizenNumber": 12345678901, "birthDate": "2002-01-08", "active": true}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.surname").value("Yılmaz"));

        mockMvc.perform(as(adminToken, patch(customerUri + "/activation")).contentType(APPLICATION_JSON).content("""
                        {"active": false}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(as(adminToken, delete(customerUri))).andExpect(status().isNoContent());
        mockMvc.perform(as(adminToken, get(customerUri))).andExpect(status().isNotFound());
    }

    @Test
    void search_isCaseInsensitive_matchesFullName_andPages() throws Exception {
        save("Ali", "Yılmaz", 11111111111L);
        save("Ayşe", "Demir", 22222222222L);
        save("Ali", "Kaya", 33333333333L);

        mockMvc.perform(as(adminToken, get("/api/customers")).param("search", "ALI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2));

        mockMvc.perform(as(adminToken, get("/api/customers")).param("search", "ali kaya"))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].surname").value("Kaya"));

        mockMvc.perform(as(adminToken, get("/api/customers")).param("size", "2").param("sort", "surname,asc"))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].surname").value("Demir"))
                .andExpect(jsonPath("$.page.totalPages").value(2));

        mockMvc.perform(as(adminToken, get("/api/customers")).param("sort", "password"))
                .andExpect(status().isBadRequest());
    }

    // ---- login and permissions ----

    @Test
    void requestsWithoutToken_areRejected() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Bu işlem için giriş yapmalısınız."));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content("""
                        {"username": "admin", "password": "wrong-password"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Kullanıcı adı veya şifre hatalı."));

        mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content("""
                        {"username": "nobody", "password": "whatever1"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Kullanıcı adı veya şifre hatalı."));
    }

    @Test
    void demoUser_canRead_butCannotChangeAnything() throws Exception {
        save("Ali", "Yılmaz", 11111111111L);
        String demoToken = tokenFrom(post("/api/auth/demo"));

        mockMvc.perform(as(demoToken, get("/api/customers")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1));

        mockMvc.perform(as(demoToken, post("/api/customers")).contentType(APPLICATION_JSON).content("""
                        {"name": "Kübra", "surname": "Uçar", "citizenNumber": 12345678901}
                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Bu işlem için yetkiniz yok."));
    }
}
