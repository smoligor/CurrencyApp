package com.currency.currencyapp.service;

import com.currency.currencyapp.domain.Transaction;
import com.currency.currencyapp.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for sending notifications to users.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send deposit notification to user.
     *
     * @param userId       the user ID
     * @param transaction  the transaction
     * @param success      whether the transaction was successful
     * @param errorMessage error message if failed
     */
    public void sendDepositNotification(String userId, Transaction transaction, boolean success, String errorMessage) {
        String message = success ? "Deposit successful" : "Deposit failed";
        log.info("Notification: {} for user {}, amount: {} {}, transaction: {}",
            message, userId, transaction.getAmount(), transaction.getCurrency(), transaction.getId());

        NotificationDto notification = NotificationDto.builder()
            .transactionId(transaction.getId())
            .status(transaction.getStatus())
            .message(message)
            .details(success ? String.format("Deposited %s %s", transaction.getAmount(), transaction.getCurrency()) : errorMessage)
            .build();

        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", notification);
    }

    /**
     * Send withdraw notification to user.
     *
     * @param userId       the user ID
     * @param transaction  the transaction
     * @param success      whether the transaction was successful
     * @param errorMessage error message if failed
     */
    public void sendWithdrawNotification(String userId, Transaction transaction, boolean success, String errorMessage) {
        String message = success ? "Withdraw successful" : "Withdraw failed";
        log.info("Notification: {} for user {}, amount: {} {}, transaction: {}",
            message, userId, transaction.getAmount(), transaction.getCurrency(), transaction.getId());

        NotificationDto notification = NotificationDto.builder()
            .transactionId(transaction.getId())
            .status(transaction.getStatus())
            .message(message)
            .details(success ? String.format("Withdrew %s %s", transaction.getAmount(), transaction.getCurrency()) : errorMessage)
            .build();

        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", notification);
    }
}
