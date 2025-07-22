package com.currency.currencyapp.dto;

import com.currency.currencyapp.domain.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for balance responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceResponse {

    private String userId;
    private Currency requestedCurrency;
    private BigDecimal totalBalance; // Total balance converted to requested currency
    private Map<Currency, BigDecimal> accountBalances; // Individual balances for USD and TRY accounts
}
