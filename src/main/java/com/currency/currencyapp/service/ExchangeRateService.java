package com.currency.currencyapp.service;

import com.currency.currencyapp.domain.Currency;
import com.currency.currencyapp.dto.ExternalExchangeRateResponse;
import com.currency.currencyapp.exception.ExchangeRateNotAvailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service for managing exchange rates with Redis caching.
 */
@Service
@Slf4j
public class ExchangeRateService {

    private final RestTemplate restTemplate;
    private final ExchangeRateService self;

    @Value("${exchange-rate.api.url}")
    private String exchangeRateApiUrl;

    public ExchangeRateService(RestTemplate restTemplate, @Lazy ExchangeRateService self) {
        this.restTemplate = restTemplate;
        this.self = self;
    }

    /**
     * Get exchange rate between two currencies.
     *
     * @param fromCurrency source currency
     * @param toCurrency   target currency
     * @return exchange rate
     */
    @Cacheable(value = "exchange_rates", key = "#fromCurrency.name() + '_' + #toCurrency.name()")
    public BigDecimal getExchangeRate(Currency fromCurrency, Currency toCurrency) {
        if (fromCurrency == toCurrency) {
            return BigDecimal.ONE;
        }

        // Fetch from external API (Spring Cache with Redis will handle caching)
        return fetchExchangeRate(fromCurrency, toCurrency);
    }

    /**
     * Convert amount from one currency to another.
     *
     * @param amount       the amount to convert
     * @param fromCurrency source currency
     * @param toCurrency   target currency
     * @return converted amount
     */
    public BigDecimal convertAmount(BigDecimal amount, Currency fromCurrency, Currency toCurrency) {
        if (fromCurrency == toCurrency) {
            return amount;
        }

        BigDecimal exchangeRate = self.getExchangeRate(fromCurrency, toCurrency);
        return amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Fetch exchange rate from external API.
     *
     * @param fromCurrency source currency
     * @param toCurrency   target currency
     * @return exchange rate
     */
    private BigDecimal fetchExchangeRate(Currency fromCurrency, Currency toCurrency) {
        try {
            String url = exchangeRateApiUrl + "/" + fromCurrency.name();
            log.info("Fetching exchange rates from: {}", url);

            ExternalExchangeRateResponse response = restTemplate.getForObject(url, ExternalExchangeRateResponse.class);

            if (response == null || response.getRates() == null) {
                throw new ExchangeRateNotAvailableException("Failed to fetch exchange rates from external API");
            }

            BigDecimal rate = response.getRates().get(toCurrency.name());
            if (rate == null) {
                throw new ExchangeRateNotAvailableException(
                    "Exchange rate not available for conversion from " + fromCurrency + " to " + toCurrency);
            }

            log.info("Fetched exchange rate from {} to {}: {}", fromCurrency, toCurrency, rate);
            return rate;

        } catch (Exception e) {
            log.error("Error fetching exchange rate from {} to {}: {}", fromCurrency, toCurrency, e.getMessage());
            throw new ExchangeRateNotAvailableException(
                "Unable to fetch exchange rate for " + fromCurrency + " to " + toCurrency, e);
        }
    }
}
