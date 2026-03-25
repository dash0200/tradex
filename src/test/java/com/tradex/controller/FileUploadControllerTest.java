package com.tradex.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradex.jwt.JwtUtil;
import com.tradex.service.S3Service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(FileUploadController.class)
@AutoConfigureMockMvc(addFilters = false)
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private S3Service s3Service;

    @Autowired
    private ObjectMapper objectMapper;

    // ✅ VALID CASE
    @Test
    @WithMockUser(username = "test-user")
    void shouldUploadSuccessfully_whenValidTrade() throws Exception {

        when(s3Service.uploadTrade(org.mockito.ArgumentMatchers.any()))
                .thenReturn("test-key");

        String json = """
        {
          "tradeType": "BUY",
          "portfolioId": "PF1001",
          "brokerId": "BRK789",
          "symbol": "INFY",
          "quantity": 100,
          "price": 1450.75,
          "currency": "INR",
          "tradeDate": "2026-03-09",
          "exchange": "NSE",
          "clientId": "CLT456"
        }
        """;

        mockMvc.perform(post("/api/files/upload/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("File uploaded successfully"))
                .andExpect(jsonPath("$.s3Key").value("test-key"));
    }

    // ❌ INVALID tradeType
    @Test
    void shouldFail_whenTradeTypeInvalid() throws Exception {

        String json = """
        {
          "tradeType": "INVALID",
          "portfolioId": "PF1001",
          "brokerId": "BRK789",
          "symbol": "INFY",
          "quantity": 100,
          "price": 1450.75,
          "currency": "INR",
          "tradeDate": "2026-03-09",
          "exchange": "NSE",
          "clientId": "CLT456"
        }
        """;

        mockMvc.perform(post("/api/files/upload/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("tradeType"));
    }

    // ❌ MULTIPLE ERRORS
    @Test
    void shouldFail_whenMultipleFieldsInvalid() throws Exception {

        String json = """
        {
          "tradeType": "XYZ",
          "portfolioId": "",
          "brokerId": "BRK@123",
          "symbol": "infy",
          "quantity": 0,
          "price": 0,
          "currency": "EUR",
          "tradeDate": "2030-01-01",
          "exchange": "NYSE",
          "clientId": ""
        }
        """;

        mockMvc.perform(post("/api/files/upload/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(org.hamcrest.Matchers.greaterThan(5)));
    }

    // ❌ MISSING REQUIRED FIELD
    @Test
    void shouldFail_whenMissingFields() throws Exception {

        String json = "{}";

        mockMvc.perform(post("/api/files/upload/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    // ❌ INVALID DATE FORMAT
    @Test
    void shouldFail_whenInvalidDateFormat() throws Exception {

        String json = """
        {
          "tradeType": "BUY",
          "portfolioId": "PF1001",
          "brokerId": "BRK789",
          "symbol": "INFY",
          "quantity": 100,
          "price": 1450.75,
          "currency": "INR",
          "tradeDate": "09-03-2026",
          "exchange": "NSE",
          "clientId": "CLT456"
        }
        """;

        mockMvc.perform(post("/api/files/upload/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}