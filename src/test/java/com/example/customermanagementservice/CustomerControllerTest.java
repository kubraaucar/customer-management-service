package com.example.customermanagementservice;

import com.example.customermanagementservice.controller.CustomerController;
import com.example.customermanagementservice.dto.CustomerRequest;
import com.example.customermanagementservice.dto.CustomerResponse;
import com.example.customermanagementservice.exceptions.CustomerNotFoundException;
import com.example.customermanagementservice.exceptions.DuplicateCitizenNumberException;
import com.example.customermanagementservice.exceptions.InvalidSortPropertyException;
import com.example.customermanagementservice.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests the HTTP layer: URLs, status codes, JSON bodies and CORS (the service is mocked).
 */
@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    private static final long CITIZEN_NUMBER = 12345678901L;
    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String VALID_BODY = """
            {"name": "Kubra", "surname": "Ucar", "citizenNumber": 12345678901, "birthDate": "2002-01-08", "active": true}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    private static CustomerResponse response(boolean active) {
        return new CustomerResponse(ID, "Kubra", "Ucar", CITIZEN_NUMBER, LocalDate.of(2002, 1, 8), active);
    }

    // ---- list / search ----

    @Test
    void list_returnsPageWithDefaultPagingAndSorting() throws Exception {
        when(customerService.search(eq("kub"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response(true))));

        mockMvc.perform(get("/api/customers").param("search", "kub"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Kubra"))
                .andExpect(jsonPath("$.content[0].birthDate").value("2002-01-08"))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(customerService).search(eq("kub"), pageable.capture());
        assertEquals(20, pageable.getValue().getPageSize());
        assertEquals(Sort.by("name"), pageable.getValue().getSort());
    }

    @Test
    void list_withUnknownSortField_returns400() throws Exception {
        when(customerService.search(any(), any(Pageable.class)))
                .thenThrow(new InvalidSortPropertyException("password"));

        mockMvc.perform(get("/api/customers").param("sort", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Bu alana göre sıralama yapılamaz: password"));
    }

    // ---- get by id ----

    @Test
    void getById_returns200() throws Exception {
        when(customerService.getById(ID)).thenReturn(response(true));

        mockMvc.perform(get("/api/customers/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citizenNumber").value(CITIZEN_NUMBER));
    }

    @Test
    void getById_whenMissing_returns404WithTurkishMessage() throws Exception {
        when(customerService.getById(ID)).thenThrow(new CustomerNotFoundException(ID));

        mockMvc.perform(get("/api/customers/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Müşteri bulunamadı: " + ID));
    }

    @Test
    void getById_withInvalidId_returns400() throws Exception {
        mockMvc.perform(get("/api/customers/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    // ---- create ----

    @Test
    void create_withValidBody_returns201WithLocation() throws Exception {
        when(customerService.create(any(CustomerRequest.class))).thenReturn(response(true));

        mockMvc.perform(post("/api/customers").contentType(APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/customers/" + ID)))
                .andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    void create_acceptsIsActiveAsFieldName() throws Exception {
        when(customerService.create(any(CustomerRequest.class))).thenReturn(response(true));

        mockMvc.perform(post("/api/customers").contentType(APPLICATION_JSON).content("""
                        {"name": "Kubra", "surname": "Ucar", "citizenNumber": 12345678901, "isActive": true}
                        """))
                .andExpect(status().isCreated());

        ArgumentCaptor<CustomerRequest> captor = ArgumentCaptor.forClass(CustomerRequest.class);
        verify(customerService).create(captor.capture());
        assertTrue(captor.getValue().isActive());
    }

    @Test
    void create_withInvalidFields_returns400WithTurkishFieldErrors() throws Exception {
        mockMvc.perform(post("/api/customers").contentType(APPLICATION_JSON).content("""
                        {"name": "", "citizenNumber": 1234, "birthDate": "2999-01-01"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Gönderilen bilgilerde hata var."))
                .andExpect(jsonPath("$.errors.name").value("Ad boş olamaz."))
                .andExpect(jsonPath("$.errors.surname").value("Soyad boş olamaz."))
                .andExpect(jsonPath("$.errors.citizenNumber").value("TC kimlik numarası 11 haneli olmalıdır."))
                .andExpect(jsonPath("$.errors.birthDate").value("Doğum tarihi geçmişte bir tarih olmalıdır."));

        verify(customerService, never()).create(any());
    }

    @Test
    void create_withMalformedJson_returns400WithTurkishMessage() throws Exception {
        mockMvc.perform(post("/api/customers").contentType(APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("İstek gövdesi okunamadı. JSON biçimini kontrol edin."));
    }

    @Test
    void create_withDuplicateCitizenNumber_returns409() throws Exception {
        when(customerService.create(any(CustomerRequest.class)))
                .thenThrow(new DuplicateCitizenNumberException(CITIZEN_NUMBER));

        mockMvc.perform(post("/api/customers").contentType(APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("zaten var")));
    }

    // ---- update / activation ----

    @Test
    void update_returns200() throws Exception {
        when(customerService.update(eq(ID), any(CustomerRequest.class))).thenReturn(response(true));

        mockMvc.perform(put("/api/customers/{id}", ID).contentType(APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kubra"));
    }

    @Test
    void update_whenMissing_returns404() throws Exception {
        when(customerService.update(eq(ID), any(CustomerRequest.class))).thenThrow(new CustomerNotFoundException(ID));

        mockMvc.perform(put("/api/customers/{id}", ID).contentType(APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void setActivation_returns200WithNewState() throws Exception {
        when(customerService.setActive(ID, false)).thenReturn(response(false));

        mockMvc.perform(patch("/api/customers/{id}/activation", ID).contentType(APPLICATION_JSON).content("""
                        {"active": false}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void setActivation_withoutValue_returns400() throws Exception {
        mockMvc.perform(patch("/api/customers/{id}/activation", ID).contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.active").exists());

        verify(customerService, never()).setActive(any(), anyBoolean());
    }

    // ---- delete ----

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/customers/{id}", ID))
                .andExpect(status().isNoContent());

        verify(customerService).delete(ID);
    }

    @Test
    void delete_whenMissing_returns404() throws Exception {
        doThrow(new CustomerNotFoundException(ID)).when(customerService).delete(ID);

        mockMvc.perform(delete("/api/customers/{id}", ID))
                .andExpect(status().isNotFound());
    }

    // ---- general errors ----

    @Test
    void unknownUrl_returns404WithTurkishMessage() throws Exception {
        mockMvc.perform(get("/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("İstenen adres bulunamadı."));
    }

    @Test
    void unsupportedMethod_returns405() throws Exception {
        mockMvc.perform(post("/api/customers/{id}", ID))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void unexpectedError_returns500WithoutInternalDetails() throws Exception {
        when(customerService.getById(ID)).thenThrow(new IllegalStateException("secret database detail"));

        mockMvc.perform(get("/api/customers/{id}", ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Beklenmeyen bir hata oluştu."))
                .andExpect(content().string(not(containsString("secret"))));
    }

    // ---- CORS ----

    @Test
    void cors_allowsTheFrontendDevServer() throws Exception {
        mockMvc.perform(options("/api/customers")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void cors_rejectsOtherOrigins() throws Exception {
        mockMvc.perform(options("/api/customers")
                        .header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "DELETE"))
                .andExpect(status().isForbidden());
    }
}
