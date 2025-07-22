package com.currency.currencyapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EnableAsync
public class CurrencyAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(CurrencyAppApplication.class, args);
    }

}
