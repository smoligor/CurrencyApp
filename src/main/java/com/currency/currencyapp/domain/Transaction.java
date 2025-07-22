package com.currency.currencyapp.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a transaction in the system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transactions")
public class Transaction {

    @Id
    private String id;

    private String userId; // Keycloak user ID
    private String accountId;

    private TransactionType type;
    private TransactionStatus status;

    private Currency currency;
    private BigDecimal amount;

    // For exchange transactions
    private Currency targetCurrency;
    private BigDecimal targetAmount;
    private BigDecimal exchangeRate;

    private String description;
    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
