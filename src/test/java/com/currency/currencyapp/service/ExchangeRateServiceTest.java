package com.currency.currencyapp.service;

import com.currency.currencyapp.domain.Currency;
import com.currency.currencyapp.dto.ExternalExchangeRateResponse;
import com.currency.currencyapp.exception.ExchangeRateNotAvailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(exchangeRateService, "exchangeRateApiUrl", "https://api.exchangerate-api.com/v4/latest");
        ReflectionTestUtils.setField(exchangeRateService, "self", exchangeRateService);
    }

    @Test
    void getExchangeRate_SameCurrency_ReturnsOne() {
        // When
        BigDecimal result = exchangeRateService.getExchangeRate(Currency.USD, Currency.USD);

        // Then
        assertEquals(BigDecimal.ONE, result);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void getExchangeRate_FetchesFromAPI_ReturnsRate() {
        // Given
        Map<String, BigDecimal> apiRates = new HashMap<>();
        apiRates.put("TRY", new BigDecimal("30.0"));

        ExternalExchangeRateResponse apiResponse = ExternalExchangeRateResponse.builder()
            .base("USD")
            .rates(apiRates)
            .build();

        when(restTemplate.getForObject(anyString(), eq(ExternalExchangeRateResponse.class)))
            .thenReturn(apiResponse);

        // When
        BigDecimal result = exchangeRateService.getExchangeRate(Currency.USD, Currency.TRY);

        // Then
        assertEquals(new BigDecimal("30.0"), result);
        verify(restTemplate, times(1)).getForObject(anyString(), eq(ExternalExchangeRateResponse.class));
    }

    @Test
    void getExchangeRate_APIFailure_ThrowsException() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(ExternalExchangeRateResponse.class)))
            .thenThrow(new RuntimeException("API Error"));

        // When & Then
        assertThrows(ExchangeRateNotAvailableException.class,
            () -> exchangeRateService.getExchangeRate(Currency.USD, Currency.TRY));
    }

    @Test
    void convertAmount_SameCurrency_ReturnsOriginalAmount() {
        // Given
        BigDecimal amount = new BigDecimal("100.00");

        // When
        BigDecimal result = exchangeRateService.convertAmount(amount, Currency.USD, Currency.USD);

        // Then
        assertEquals(amount, result);
    }

    @Test
    void convertAmount_DifferentCurrency_ConvertsAmount() {
        // Given
        BigDecimal amount = new BigDecimal("100.00");

        Map<String, BigDecimal> apiRates = new HashMap<>();
        apiRates.put("TRY", new BigDecimal("30.0"));

        ExternalExchangeRateResponse apiResponse = ExternalExchangeRateResponse.builder()
            .base("USD")
            .rates(apiRates)
            .build();

        when(restTemplate.getForObject(anyString(), eq(ExternalExchangeRateResponse.class)))
            .thenReturn(apiResponse);

        // When
        BigDecimal result = exchangeRateService.convertAmount(amount, Currency.USD, Currency.TRY);

        // Then
        assertEquals(new BigDecimal("3000.00"), result);
    }
}
