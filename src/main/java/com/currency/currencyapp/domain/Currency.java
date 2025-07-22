package com.currency.currencyapp.domain;

import lombok.Getter;

/**
 * Enumeration representing supported currencies in the system.
 */
@Getter
public enum Currency {
    USD("United States Dollar"),
    TRY("Turkish Lira");

    private final String displayName;

    Currency(String displayName) {
        this.displayName = displayName;
    }

}
