package com.currency.currencyapp.dto;

import com.currency.currencyapp.domain.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for currency exchange requests.
 * According to requirements, exchange is allowed only between USD and TRY currencies.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRequest {

    @NotNull(message = "From currency is required")
    private Currency fromCurrency;

    @NotNull(message = "To currency is required")
    private Currency toCurrency;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String description;
}
