package com.currency.currencyapp.service;

import com.currency.currencyapp.domain.Account;
import com.currency.currencyapp.domain.Currency;
import com.currency.currencyapp.dto.BalanceResponse;
import com.currency.currencyapp.exception.AccountNotFoundException;
import com.currency.currencyapp.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private AccountService accountService;

    private Account usdAccount;
    private Account tryAccount;
    private final String userId = "test-user-123";

    @BeforeEach
    void setUp() {
        usdAccount = Account.builder()
            .id("usd-account-id")
            .userId(userId)
            .currency(Currency.USD)
            .balance(new BigDecimal("1000.00"))
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        tryAccount = Account.builder()
            .id("try-account-id")
            .userId(userId)
            .currency(Currency.TRY)
            .balance(new BigDecimal("5000.00"))
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    @Test
    void testGetOrCreateAccount_ExistingAccount() {
        // Given
        when(accountRepository.findByUserIdAndCurrency(userId, Currency.USD)).thenReturn(Optional.of(usdAccount));

        // When
        Account result = accountService.getOrCreateAccount(userId, Currency.USD);

        // Then
        assertNotNull(result);
        assertEquals(usdAccount, result);
        verify(accountRepository, times(1)).findByUserIdAndCurrency(userId, Currency.USD);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void testGetOrCreateAccount_NewAccount() {
        // Given
        when(accountRepository.findByUserIdAndCurrency(userId, Currency.USD)).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenReturn(usdAccount);

        // When
        Account result = accountService.getOrCreateAccount(userId, Currency.USD);

        // Then
        assertNotNull(result);
        verify(accountRepository, times(1)).findByUserIdAndCurrency(userId, Currency.USD);
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void testGetAccountByUserIdAndCurrency_Success() {
        // Given
        when(accountRepository.findByUserIdAndCurrency(userId, Currency.USD)).thenReturn(Optional.of(usdAccount));

        // When
        Account result = accountService.getAccountByUserIdAndCurrency(userId, Currency.USD);

        // Then
        assertNotNull(result);
        assertEquals(usdAccount, result);
    }

    @Test
    void testGetAccountByUserIdAndCurrency_NotFound() {
        // Given
        when(accountRepository.findByUserIdAndCurrency(userId, Currency.USD)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AccountNotFoundException.class, 
            () -> accountService.getAccountByUserIdAndCurrency(userId, Currency.USD));
    }

    @Test
    void testGetBalance_WithExistingAccounts() {
        // Given
        List<Account> accounts = Arrays.asList(usdAccount, tryAccount);
        when(accountRepository.findByUserId(userId)).thenReturn(accounts);
        when(exchangeRateService.getExchangeRate(Currency.TRY, Currency.USD)).thenReturn(new BigDecimal("0.037"));

        // When
        BalanceResponse result = accountService.getBalance(userId, Currency.USD);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(Currency.USD, result.getRequestedCurrency());
        assertEquals(new BigDecimal("1185.00"), result.getTotalBalance()); // 1000 + (5000 * 0.037)
        assertEquals(2, result.getAccountBalances().size());
        assertEquals(new BigDecimal("1000.00"), result.getAccountBalances().get(Currency.USD));
        assertEquals(new BigDecimal("5000.00"), result.getAccountBalances().get(Currency.TRY));
    }

    @Test
    void testGetBalance_NoExistingAccounts() {
        // Given
        when(accountRepository.findByUserId(userId)).thenReturn(List.of());
        when(accountRepository.save(any(Account.class))).thenReturn(usdAccount);

        // When
        BalanceResponse result = accountService.getBalance(userId, Currency.USD);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(Currency.USD, result.getRequestedCurrency());
        verify(accountRepository, times(2)).save(any(Account.class)); // For USD and TRY accounts
    }

    @Test
    void testAddToBalance() {
        // Given
        when(accountRepository.findByUserIdAndCurrency(userId, Currency.USD)).thenReturn(Optional.of(usdAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(usdAccount);

        // When
        Account result = accountService.addToBalance(userId, Currency.USD, new BigDecimal("500.00"));

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("1500.00"), result.getBalance());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void testSubtractFromBalance_Success() {
        // Given
        when(accountRepository.findByUserIdAndCurrency(userId, Currency.USD)).thenReturn(Optional.of(usdAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(usdAccount);

        // When
        Account result = accountService.subtractFromBalance(userId, Currency.USD, new BigDecimal("500.00"));

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("500.00"), result.getBalance());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void testSubtractFromBalance_InsufficientFunds() {
        // Given
        when(accountRepository.findByUserIdAndCurrency(userId, Currency.USD)).thenReturn(Optional.of(usdAccount));

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> accountService.subtractFromBalance(userId, Currency.USD, new BigDecimal("1500.00")));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void testCreateAccountsForUser() {
        // Given
        when(accountRepository.findByUserIdAndCurrency(userId, Currency.USD)).thenReturn(Optional.empty());
        when(accountRepository.findByUserIdAndCurrency(userId, Currency.TRY)).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenReturn(usdAccount);

        // When
        accountService.createAccountsForUser(userId);

        // Then
        verify(accountRepository, times(2)).save(any(Account.class)); // USD and TRY accounts
    }
}
