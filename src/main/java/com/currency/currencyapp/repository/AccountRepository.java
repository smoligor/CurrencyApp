package com.currency.currencyapp.repository;

import com.currency.currencyapp.domain.Account;
import com.currency.currencyapp.domain.Currency;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Account entities.
 */
@Repository
public interface AccountRepository extends MongoRepository<Account, String> {

    /**
     * Find a specific account by user ID and currency.
     */
    Optional<Account> findByUserIdAndCurrency(String userId, Currency currency);
    
    /**
     * Find all accounts for a user.
     */
    List<Account> findByUserId(String userId);
}