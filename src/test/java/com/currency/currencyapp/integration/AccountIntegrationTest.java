package com.currency.currencyapp.integration;

import com.currency.currencyapp.config.TestSecurityConfig;
import com.currency.currencyapp.domain.Account;
import com.currency.currencyapp.domain.Currency;
import com.currency.currencyapp.dto.DepositRequest;
import com.currency.currencyapp.dto.ExchangeRequest;
import com.currency.currencyapp.dto.WithdrawRequest;
import com.currency.currencyapp.repository.AccountRepository;
import com.currency.currencyapp.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {TestApplication.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class AccountIntegrationTest {

    @Container
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
        accountRepository.deleteAll();
        transactionRepository.deleteAll();
    }

    @Test
    void getBalance_NewUser_CreatesAccountAndReturnsZeroBalance() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/accounts/balance")
                .with(httpBasic("testuser", "testpass"))
                .param("currency", "USD"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requestedCurrency").value("USD"))
            .andExpect(jsonPath("$.totalBalance").value(0));

        // Verify USD account was created
        Optional<Account> usdAccount = accountRepository.findByUserIdAndCurrency("testuser", Currency.USD);
        assertTrue(usdAccount.isPresent());
        assertEquals(BigDecimal.ZERO, usdAccount.get().getBalance());
    }

    @Test
    void deposit_ValidRequest_ReturnsAccepted() throws Exception {
        // Given
        DepositRequest request = DepositRequest.builder()
            .currency(Currency.USD)
            .amount(new BigDecimal("100.00"))
            .description("Integration test deposit")
            .build();

        // When & Then
        mockMvc.perform(post("/api/v1/accounts/deposit")
                .with(httpBasic("testuser", "testpass"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted())
            .andExpect(content().string("Deposit request accepted. You will be notified once processing is complete."));
    }

    @Test
    void withdraw_ValidRequest_ReturnsAccepted() throws Exception {
        // Given - First create an account with some balance
        Account account = Account.builder()
            .userId("testuser")
            .currency(Currency.USD)
            .balance(new BigDecimal("500.00"))
            .createdAt(java.time.LocalDateTime.now())
            .updatedAt(java.time.LocalDateTime.now())
            .build();
        accountRepository.save(account);

        WithdrawRequest request = WithdrawRequest.builder()
            .currency(Currency.USD)
            .amount(new BigDecimal("100.00"))
            .description("Integration test withdraw")
            .build();

        // When & Then
        mockMvc.perform(post("/api/v1/accounts/withdraw")
                .with(httpBasic("testuser", "testpass"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted())
            .andExpect(content().string("Withdraw request accepted. You will be notified once processing is complete."));
    }

    @Test
    void exchange_ValidRequest_ReturnsSuccess() throws Exception {
        // Given - Create USD account with balance
        Account usdAccount = Account.builder()
            .userId("testuser")
            .currency(Currency.USD)
            .balance(new BigDecimal("1000.00"))
            .createdAt(java.time.LocalDateTime.now())
            .updatedAt(java.time.LocalDateTime.now())
            .build();
        accountRepository.save(usdAccount);

        ExchangeRequest request = ExchangeRequest.builder()
            .fromCurrency(Currency.USD)
            .toCurrency(Currency.TRY)
            .amount(new BigDecimal("100.00"))
            .description("Integration test exchange")
            .build();

        // When & Then
        mockMvc.perform(post("/api/v1/accounts/exchange")
                .with(httpBasic("testuser", "testpass"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value(containsString("Exchange successful")));
    }

    @Test
    void getBalance_Unauthenticated_ReturnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/accounts/balance"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deposit_InvalidAmount_ReturnsBadRequest() throws Exception {
        // Given
        DepositRequest request = DepositRequest.builder()
            .currency(Currency.USD)
            .amount(new BigDecimal("-50.00")) // Negative amount
            .description("Invalid deposit")
            .build();

        // When & Then
        mockMvc.perform(post("/api/v1/accounts/deposit")
                .with(httpBasic("testuser", "testpass"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deposit_MissingCurrency_ReturnsBadRequest() throws Exception {
        // Given
        DepositRequest request = DepositRequest.builder()
            .amount(new BigDecimal("100.00"))
            .description("Missing currency")
            .build(); // Currency is null

        // When & Then
        mockMvc.perform(post("/api/v1/accounts/deposit")
                .with(httpBasic("testuser", "testpass"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void exchange_InsufficientFunds_ReturnsFailedTransaction() throws Exception {
        // Given - Create account with insufficient balance
        Account account = Account.builder()
            .userId("pooruser") // Different user with limited funds
            .currency(Currency.USD)
            .balance(new BigDecimal("50.00")) // Less than requested
            .createdAt(java.time.LocalDateTime.now())
            .updatedAt(java.time.LocalDateTime.now())
            .build();
        accountRepository.save(account);

        ExchangeRequest request = ExchangeRequest.builder()
            .fromCurrency(Currency.USD)
            .toCurrency(Currency.TRY)
            .amount(new BigDecimal("100.00")) // More than available
            .description("Exchange with insufficient funds")
            .build();

        // When & Then
        mockMvc.perform(post("/api/v1/accounts/exchange")
                .with(httpBasic("pooruser", "testpass"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getBalance_MultiCurrency_ReturnsConvertedTotal() throws Exception {
        // Given - Create accounts for both currencies
        Account usdAccount = Account.builder()
            .userId("testuser")
            .currency(Currency.USD)
            .balance(new BigDecimal("100.00"))
            .createdAt(java.time.LocalDateTime.now())
            .updatedAt(java.time.LocalDateTime.now())
            .build();
        accountRepository.save(usdAccount);

        Account tryAccount = Account.builder()
            .userId("testuser")
            .currency(Currency.TRY)
            .balance(new BigDecimal("1000.00"))
            .createdAt(java.time.LocalDateTime.now())
            .updatedAt(java.time.LocalDateTime.now())
            .build();
        accountRepository.save(tryAccount);

        // When & Then - Request balance in USD (should convert TRY to USD)
        mockMvc.perform(get("/api/v1/accounts/balance")
                .with(httpBasic("testuser", "testpass"))
                .param("currency", "USD"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requestedCurrency").value("USD"))
            .andExpect(jsonPath("$.accountBalances.USD").value(100.00))
            .andExpect(jsonPath("$.accountBalances.TRY").value(1000.00));
    }
}
