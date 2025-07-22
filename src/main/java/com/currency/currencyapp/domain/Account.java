package com.currency.currencyapp.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a user account for a specific currency.
 * Each user has separate accounts for USD and TRY currencies.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "accounts")
@CompoundIndex(name = "user_currency_idx", def = "{'userId': 1, 'currency': 1}", unique = true)
public class Account {

    @Id
    private String id;

    @Version
    private Long version;

    private String userId; // Keycloak user ID
    
    private Currency currency; // The currency this account handles (USD or TRY)
    
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
