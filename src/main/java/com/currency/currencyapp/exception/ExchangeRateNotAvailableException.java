package com.currency.currencyapp.exception;

/**
 * Exception thrown when exchange rate information is not available.
 */
public class ExchangeRateNotAvailableException extends RuntimeException {

    public ExchangeRateNotAvailableException(String message) {
        super(message);
    }

    public ExchangeRateNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
