package com.currency.currencyapp.service;

import com.currency.currencyapp.domain.Account;
import com.currency.currencyapp.domain.Currency;
import com.currency.currencyapp.domain.Transaction;
import com.currency.currencyapp.domain.TransactionStatus;
import com.currency.currencyapp.domain.TransactionType;
import com.currency.currencyapp.dto.DepositRequest;
import com.currency.currencyapp.dto.ExchangeRequest;
import com.currency.currencyapp.dto.TransactionResponse;
import com.currency.currencyapp.dto.WithdrawRequest;
import com.currency.currencyapp.exception.InsufficientFundsException;
import com.currency.currencyapp.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing transactions with separate currency accounts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ExchangeRateService exchangeRateService;
    private final NotificationService notificationService;
    private final AccountService accountService;

    /**
     * Process deposit transaction asynchronously.
     */
    @Async("taskExecutor")
    public void processDeposit(String userId, DepositRequest request) {
        log.info("Processing deposit for user: {}, amount: {} {}", userId, request.getAmount(), request.getCurrency());

        Transaction transaction = createTransaction(userId, TransactionType.DEPOSIT, request.getCurrency(),
            request.getAmount(), request.getDescription());

        try {
            // Simulate processing time (e.g., payment gateway interaction)
            Thread.sleep(1000);

            // Add amount to specific currency account
            accountService.addToBalance(userId, request.getCurrency(), request.getAmount());

            // Mark transaction as successful
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setProcessedAt(LocalDateTime.now());
            transactionRepository.save(transaction);

            // Send notification
            notificationService.sendDepositNotification(userId, transaction, true, null);

            log.info("Deposit processed successfully for user: {}, transaction: {}", userId, transaction.getId());

            CompletableFuture.completedFuture(TransactionResponse.builder()
                .transactionId(transaction.getId())
                .status(TransactionStatus.SUCCESS)
                .message("Deposit processed successfully")
                .timestamp(LocalDateTime.now())
                .build());

        } catch (Exception e) {
            // Mark transaction as failed
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setErrorMessage(e.getMessage());
            transaction.setProcessedAt(LocalDateTime.now());
            transactionRepository.save(transaction);

            // Send failure notification
            notificationService.sendDepositNotification(userId, transaction, false, e.getMessage());

            log.error("Deposit failed for user: {}, transaction: {}, error: {}",
                userId, transaction.getId(), e.getMessage());

            CompletableFuture.completedFuture(TransactionResponse.builder()
                .transactionId(transaction.getId())
                .status(TransactionStatus.FAILED)
                .message("Deposit failed: " + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
        }
    }

    /**
     * Process withdraw transaction asynchronously.
     */
    @Async("taskExecutor")
    public void processWithdraw(String userId, WithdrawRequest request) {
        log.info("Processing withdraw for user: {}, amount: {} {}", userId, request.getAmount(), request.getCurrency());

        Transaction transaction = createTransaction(userId, TransactionType.WITHDRAW, request.getCurrency(),
            request.getAmount(), request.getDescription());

        try {
            // Check if sufficient funds are available
            Account account = accountService.getAccountByUserIdAndCurrency(userId, request.getCurrency());
            if (account.getBalance().compareTo(request.getAmount()) < 0) {
                throw new InsufficientFundsException(
                    String.format("Insufficient funds. Available: %s, Requested: %s", account.getBalance(), request.getAmount()));
            }

            // Simulate processing time (e.g., bank transfer)
            Thread.sleep(1500);

            // Subtract amount from specific currency account
            accountService.subtractFromBalance(userId, request.getCurrency(), request.getAmount());

            // Mark transaction as successful
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setProcessedAt(LocalDateTime.now());
            transactionRepository.save(transaction);

            // Send notification
            notificationService.sendWithdrawNotification(userId, transaction, true, null);

            log.info("Withdraw processed successfully for user: {}, transaction: {}", userId, transaction.getId());

            CompletableFuture.completedFuture(TransactionResponse.builder()
                .transactionId(transaction.getId())
                .status(TransactionStatus.SUCCESS)
                .message("Withdraw processed successfully")
                .timestamp(LocalDateTime.now())
                .build());

        } catch (Exception e) {
            // Mark transaction as failed
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setErrorMessage(e.getMessage());
            transaction.setProcessedAt(LocalDateTime.now());
            transactionRepository.save(transaction);

            // Send failure notification
            notificationService.sendWithdrawNotification(userId, transaction, false, e.getMessage());

            log.error("Withdraw failed for user: {}, transaction: {}, error: {}",
                userId, transaction.getId(), e.getMessage());

            CompletableFuture.completedFuture(TransactionResponse.builder()
                .transactionId(transaction.getId())
                .status(TransactionStatus.FAILED)
                .message("Withdraw failed: " + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
        }
    }

    /**
     * Process currency exchange transaction (SYNCHRONOUS - no notifications needed).
     */
    @Transactional
    public TransactionResponse processExchange(String userId, ExchangeRequest request) {
        log.info("Processing exchange for user: {}, {} {} to {}",
            userId, request.getAmount(), request.getFromCurrency(), request.getToCurrency());

        // Validate that currencies are different
        if (request.getFromCurrency().equals(request.getToCurrency())) {
            throw new IllegalArgumentException("Cannot exchange currency to the same currency: " + request.getFromCurrency());
        }

        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(request.getFromCurrency(), request.getToCurrency());
        BigDecimal convertedAmount = exchangeRateService.convertAmount(request.getAmount(),
            request.getFromCurrency(), request.getToCurrency());

        Transaction fromTransaction = createExchangeTransaction(userId, TransactionType.EXCHANGE_FROM,
            request.getFromCurrency(), request.getAmount(), request.getToCurrency(), convertedAmount,
            exchangeRate, request.getDescription());

        Transaction toTransaction = createExchangeTransaction(userId, TransactionType.EXCHANGE_TO,
            request.getToCurrency(), convertedAmount, request.getFromCurrency(), request.getAmount(),
            exchangeRate, request.getDescription());

        try {
            // Check if sufficient funds are available in from-currency account
            Account fromAccount = accountService.getAccountByUserIdAndCurrency(userId, request.getFromCurrency());
            if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
                // Mark transactions as failed before throwing exception
                fromTransaction.setStatus(TransactionStatus.FAILED);
                fromTransaction.setErrorMessage("Insufficient funds");
                fromTransaction.setProcessedAt(LocalDateTime.now());
                transactionRepository.save(fromTransaction);

                toTransaction.setStatus(TransactionStatus.FAILED);
                toTransaction.setErrorMessage("Insufficient funds");
                toTransaction.setProcessedAt(LocalDateTime.now());
                transactionRepository.save(toTransaction);

                throw new InsufficientFundsException(
                    String.format("Insufficient funds in %s account. Available: %s, Requested: %s", 
                        request.getFromCurrency(), fromAccount.getBalance(), request.getAmount()));
            }

            // Perform the exchange
            accountService.subtractFromBalance(userId, request.getFromCurrency(), request.getAmount());
            accountService.addToBalance(userId, request.getToCurrency(), convertedAmount);

            // Mark transactions as successful
            fromTransaction.setStatus(TransactionStatus.SUCCESS);
            fromTransaction.setProcessedAt(LocalDateTime.now());
            transactionRepository.save(fromTransaction);

            toTransaction.setStatus(TransactionStatus.SUCCESS);
            toTransaction.setProcessedAt(LocalDateTime.now());
            transactionRepository.save(toTransaction);

            log.info("Exchange processed successfully for user: {}, from transaction: {}, to transaction: {}",
                userId, fromTransaction.getId(), toTransaction.getId());

            return TransactionResponse.builder()
                .transactionId(fromTransaction.getId())
                .status(TransactionStatus.SUCCESS)
                .message(String.format("Exchange successful: %s %s to %s %s", 
                    request.getAmount(), request.getFromCurrency(), 
                    convertedAmount, request.getToCurrency()))
                .timestamp(LocalDateTime.now())
                .build();

        } catch (InsufficientFundsException e) {
            // Re-throw InsufficientFundsException to be handled by controller/global exception handler
            log.error("Exchange failed for user: {}, error: {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            // Mark transactions as failed for other exceptions
            fromTransaction.setStatus(TransactionStatus.FAILED);
            fromTransaction.setErrorMessage(e.getMessage());
            fromTransaction.setProcessedAt(LocalDateTime.now());
            transactionRepository.save(fromTransaction);

            toTransaction.setStatus(TransactionStatus.FAILED);
            toTransaction.setErrorMessage(e.getMessage());
            toTransaction.setProcessedAt(LocalDateTime.now());
            transactionRepository.save(toTransaction);

            log.error("Exchange failed for user: {}, error: {}", userId, e.getMessage());

            return TransactionResponse.builder()
                .transactionId(fromTransaction.getId())
                .status(TransactionStatus.FAILED)
                .message("Exchange failed: " + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        }
    }

    /**
     * Create a transaction record.
     */
    private Transaction createTransaction(String userId, TransactionType type, Currency currency,
                                       BigDecimal amount, String description) {
        Transaction transaction = Transaction.builder()
            .userId(userId)
            .type(type)
            .currency(currency)
            .amount(amount)
            .status(TransactionStatus.PENDING)
            .description(description)
            .createdAt(LocalDateTime.now())
            .build();

        return transactionRepository.save(transaction);
    }

    /**
     * Create an exchange transaction record.
     */
    private Transaction createExchangeTransaction(String userId, TransactionType type, Currency currency,
                                                BigDecimal amount, Currency targetCurrency, BigDecimal targetAmount,
                                                BigDecimal exchangeRate, String description) {
        Transaction transaction = Transaction.builder()
            .userId(userId)
            .type(type)
            .currency(currency)
            .amount(amount)
            .targetCurrency(targetCurrency)
            .targetAmount(targetAmount)
            .exchangeRate(exchangeRate)
            .status(TransactionStatus.PENDING)
            .description(description)
            .createdAt(LocalDateTime.now())
            .build();

        return transactionRepository.save(transaction);
    }
}
