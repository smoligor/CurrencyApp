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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for account and transaction operations.
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    /**
     * Deposit money to user's account.
     *
     * @param request deposit request
     * @return transaction response
     */
    @PostMapping("/deposit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> deposit(@Valid @RequestBody DepositRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        log.info("Deposit request received for user {}, amount: {} {}", userId, request.getAmount(), request.getCurrency());
        transactionService.processDeposit(userId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("Deposit request accepted. You will be notified once processing is complete.");
    }

    /**
     * Withdraw money from user's account.
     *
     * @param request withdraw request
     * @return transaction response
     */
    @PostMapping("/withdraw")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> withdraw(@Valid @RequestBody WithdrawRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        log.info("Withdraw request received for user {}, amount: {} {}", userId, request.getAmount(), request.getCurrency());
        transactionService.processWithdraw(userId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("Withdraw request accepted. You will be notified once processing is complete.");
    }

    /**
     * Get user's balance in requested currency.
     *
     * @param currency the requested currency (optional, defaults to USD)
     * @return balance response
     */
    @GetMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BalanceResponse> getBalance(@RequestParam(defaultValue = "USD") Currency currency) {
        String userId = SecurityUtils.getCurrentUserId();
        log.info("Balance request from user: {} in currency: {}", userId, currency);

        BalanceResponse balance = accountService.getBalance(userId, currency);
        return ResponseEntity.ok(balance);
    }

    /**
     * Exchange between currencies.
     *
     * @param request exchange request
     * @return transaction response
     */
    @PostMapping("/exchange")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TransactionResponse> exchange(@Valid @RequestBody ExchangeRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        log.info("Exchange request from user: {}, from: {} to: {}, amount: {}", userId, request.getFromCurrency(), request.getToCurrency(), request.getAmount());

        TransactionResponse response = transactionService.processExchange(userId, request);
        return ResponseEntity.ok(response);
    }
}

