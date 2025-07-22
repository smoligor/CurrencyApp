package com.currency.currencyapp.exception;

/**
 * Exception thrown when there are insufficient funds for an operation.
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}
