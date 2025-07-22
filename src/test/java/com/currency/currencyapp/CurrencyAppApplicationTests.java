package com.currency.currencyapp;

import com.currency.currencyapp.integration.TestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class CurrencyAppApplicationTests {

    @Test
    void contextLoads() {
    }

}
