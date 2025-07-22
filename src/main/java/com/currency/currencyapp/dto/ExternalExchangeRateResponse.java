package com.currency.currencyapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for external exchange rate API response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalExchangeRateResponse {

    private String base;
    private String date;
    private Map<String, BigDecimal> rates;

}
