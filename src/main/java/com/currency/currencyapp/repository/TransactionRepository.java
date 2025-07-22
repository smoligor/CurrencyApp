package com.currency.currencyapp.repository;

import com.currency.currencyapp.domain.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Transaction entities.
 */
@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {

}
