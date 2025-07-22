package com.currency.currencyapp.service;

import com.currency.currencyapp.domain.Account;
import com.currency.currencyapp.domain.Currency;
import com.currency.currencyapp.dto.BalanceResponse;
import com.currency.currencyapp.exception.AccountNotFoundException;
import com.currency.currencyapp.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final ExchangeRateService exchangeRateService;

    // Supported currencies according to requirements (only USD and TRY)
    private static final Currency[] SUPPORTED_CURRENCIES = {Currency.USD, Currency.TRY};

    /**
     * Validate that the currency is supported by the system.
     * According to requirements, only USD and TRY are supported.
     */
    private void validateSupportedCurrency(Currency currency) {
        boolean isSupported = false;
        for (Currency supportedCurrency : SUPPORTED_CURRENCIES) {
            if (supportedCurrency.equals(currency)) {
                isSupported = true;
                break;
            }
        }
        if (!isSupported) {
            throw new IllegalArgumentException("Currency " + currency + " is not supported. Only USD and TRY are allowed.");
        }
    }

    /**
     * Get or create account for a user and specific currency.
     * This method implements a "lazy initialization" pattern.
     * Accounts are automatically created on their first use.
     */
    public Account getOrCreateAccount(String userId, Currency currency) {
        validateSupportedCurrency(currency);
        return accountRepository.findByUserIdAndCurrency(userId, currency)
                .orElseGet(() -> createNewAccount(userId, currency));
    }

    /**
     * Get account by user ID and currency.
     */
    public Account getAccountByUserIdAndCurrency(String userId, Currency currency) {
        validateSupportedCurrency(currency);
        return accountRepository.findByUserIdAndCurrency(userId, currency)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for user: " + userId + " and currency: " + currency));
    }

    /**
     * Get all accounts for a user.
     */
    public List<Account> getAllAccountsByUserId(String userId) {
        return accountRepository.findByUserId(userId);
    }

    /**
     * Get balance in a specific currency (with conversion if needed).
     * According to requirements, user has only USD and TRY accounts.
     */
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String userId, Currency currency) {
        validateSupportedCurrency(currency);
        
        List<Account> userAccounts = getAllAccountsByUserId(userId);
        
        // If no accounts exist, create them (only USD and TRY)
        if (userAccounts.isEmpty()) {
            createAccountsForUser(userId);
            userAccounts = getAllAccountsByUserId(userId);
        }

        BigDecimal totalBalance = BigDecimal.ZERO;
        Map<Currency, BigDecimal> accountBalances = new HashMap<>();

        // Initialize balances for both required currencies
        accountBalances.put(Currency.USD, BigDecimal.ZERO);
        accountBalances.put(Currency.TRY, BigDecimal.ZERO);

        // Calculate total balance in requested currency
        for (Account account : userAccounts) {
            BigDecimal balance = account.getBalance();
            accountBalances.put(account.getCurrency(), balance);

            if (balance == null || balance.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            if (account.getCurrency().equals(currency)) {
                totalBalance = totalBalance.add(balance);
            } else {
                BigDecimal rate = exchangeRateService.getExchangeRate(account.getCurrency(), currency);
                if (rate != null) {
                    BigDecimal convertedAmount = balance.multiply(rate);
                    totalBalance = totalBalance.add(convertedAmount);
                }
            }
        }

        return BalanceResponse.builder()
                .userId(userId)
                .requestedCurrency(currency)
                .totalBalance(totalBalance.setScale(2, java.math.RoundingMode.HALF_UP))
                .accountBalances(accountBalances)
                .build();
    }

    /**
     * Add amount to existing balance.
     */
    @Transactional
    public Account addToBalance(String userId, Currency currency, BigDecimal amount) {
        validateSupportedCurrency(currency);
        Account account = getOrCreateAccount(userId, currency);
        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        account.setUpdatedAt(LocalDateTime.now());
        return accountRepository.save(account);
    }

    /**
     * Subtract amount from existing balance.
     */
    @Transactional
    public Account subtractFromBalance(String userId, Currency currency, BigDecimal amount) {
        validateSupportedCurrency(currency);
        Account account = getAccountByUserIdAndCurrency(userId, currency);
        BigDecimal currentBalance = account.getBalance();
        BigDecimal newBalance = currentBalance.subtract(amount);
        
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Insufficient balance. Current balance: " + currentBalance + ", Requested: " + amount);
        }
        
        account.setBalance(newBalance);
        account.setUpdatedAt(LocalDateTime.now());
        return accountRepository.save(account);
    }

    /**
     * Create accounts for all supported currencies for a user.
     * According to requirements, user should have only USD and TRY accounts.
     */
    @Transactional
    public void createAccountsForUser(String userId) {
        // Create only USD and TRY accounts as per requirements
        for (Currency currency : SUPPORTED_CURRENCIES) {
            if (accountRepository.findByUserIdAndCurrency(userId, currency).isEmpty()) {
                createNewAccount(userId, currency);
            }
        }
    }

    /**
     * Create a new account for a user and specific currency.
     */
    private Account createNewAccount(String userId, Currency currency) {
        Account account = Account.builder()
                .userId(userId)
                .currency(currency)
                .balance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        log.info("Created new {} account for user: {}", currency, userId);
        return accountRepository.save(account);
    }
}
