package com.currency.currencyapp.controller;

import com.currency.currencyapp.domain.Currency;
import com.currency.currencyapp.dto.BalanceResponse;
import com.currency.currencyapp.dto.DepositRequest;
import com.currency.currencyapp.dto.ExchangeRequest;
import com.currency.currencyapp.dto.TransactionResponse;
import com.currency.currencyapp.dto.WithdrawRequest;
import com.currency.currencyapp.service.AccountService;
import com.currency.currencyapp.service.TransactionService;
import com.currency.currencyapp.util.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private TransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    private final String userId = "test-user-123";

    @BeforeEach
    void setUp() {
        // Mock SecurityUtils to return test user ID
    }

    @Test
    @WithMockUser
    void deposit_ValidRequest_ReturnsAccepted() throws Exception {
        // Given
        DepositRequest request = DepositRequest.builder()
            .currency(Currency.USD)
            .amount(new BigDecimal("100.00"))
            .description("Test deposit")
            .build();

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            // When & Then
            mockMvc.perform(post("/api/v1/accounts/deposit")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Deposit request accepted. You will be notified once processing is complete."));
        }
    }

    @Test
    @WithMockUser
    void withdraw_ValidRequest_ReturnsAccepted() throws Exception {
        // Given
        WithdrawRequest request = WithdrawRequest.builder()
            .currency(Currency.USD)
            .amount(new BigDecimal("50.00"))
            .description("Test withdraw")
            .build();

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            // When & Then
            mockMvc.perform(post("/api/v1/accounts/withdraw")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Withdraw request accepted. You will be notified once processing is complete."));
        }
    }

    @Test
    @WithMockUser
    void getBalance_ValidRequest_ReturnsBalance() throws Exception {
        // Given
        BalanceResponse balanceResponse = BalanceResponse.builder()
            .userId(userId)
            .requestedCurrency(Currency.USD)
            .totalBalance(new BigDecimal("1000.00"))
            .accountBalances(new HashMap<>())
            .build();

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            when(accountService.getBalance(userId, Currency.USD)).thenReturn(balanceResponse);

            // When & Then
            mockMvc.perform(get("/api/v1/accounts/balance")
                    .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.requestedCurrency").value("USD"))
                .andExpect(jsonPath("$.totalBalance").value(1000.00));
        }
    }

    @Test
    @WithMockUser
    void exchange_ValidRequest_ReturnsTransactionResponse() throws Exception {
        // Given
        ExchangeRequest request = ExchangeRequest.builder()
            .fromCurrency(Currency.USD)
            .toCurrency(Currency.TRY)
            .amount(new BigDecimal("100.00"))
            .description("Test exchange")
            .build();

        TransactionResponse response = TransactionResponse.builder()
            .transactionId("txn-123")
            .status(com.currency.currencyapp.domain.TransactionStatus.SUCCESS)
            .message("Exchange successful")
            .timestamp(LocalDateTime.now())
            .build();

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            when(transactionService.processExchange(eq(userId), eq(request)))
                .thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/v1/accounts/exchange")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("txn-123"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
        }
    }

    @Test
    @WithMockUser
    void deposit_InvalidAmount_ReturnsBadRequest() throws Exception {
        // Given
        DepositRequest request = DepositRequest.builder()
            .currency(Currency.USD)
            .amount(new BigDecimal("-100.00")) // Invalid negative amount
            .build();

        // When & Then
        mockMvc.perform(post("/api/v1/accounts/deposit")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getBalance_Unauthenticated_ReturnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/accounts/balance"))
            .andExpect(status().isUnauthorized());
    }
}
