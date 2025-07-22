package com.currency.currencyapp.service;

import com.currency.currencyapp.domain.Currency;
import com.currency.currencyapp.domain.Transaction;
import com.currency.currencyapp.domain.TransactionStatus;
import com.currency.currencyapp.dto.NotificationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(messagingTemplate);
    }

    @Test
    void testSendDepositNotification_Success() {
        // Given
        String userId = "test-user";
        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID().toString())
            .status(TransactionStatus.SUCCESS)
            .amount(BigDecimal.TEN)
            .currency(Currency.USD)
            .build();

        // When
        notificationService.sendDepositNotification(userId, transaction, true, null);

        // Then
        ArgumentCaptor<NotificationDto> notificationCaptor = ArgumentCaptor.forClass(NotificationDto.class);
        verify(messagingTemplate).convertAndSendToUser(
            ArgumentCaptor.forClass(String.class).capture(),
            ArgumentCaptor.forClass(String.class).capture(),
            notificationCaptor.capture()
        );

        NotificationDto capturedNotification = notificationCaptor.getValue();
        assertEquals(transaction.getId(), capturedNotification.getTransactionId());
        assertEquals(TransactionStatus.SUCCESS, capturedNotification.getStatus());
        assertEquals("Deposit successful", capturedNotification.getMessage());
    }

    @Test
    void testSendWithdrawNotification_Failure() {
        // Given
        String userId = "test-user";
        String errorMessage = "Insufficient funds";
        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID().toString())
            .status(TransactionStatus.FAILED)
            .amount(BigDecimal.TEN)
            .currency(Currency.USD)
            .build();

        // When
        notificationService.sendWithdrawNotification(userId, transaction, false, errorMessage);

        // Then
        ArgumentCaptor<NotificationDto> notificationCaptor = ArgumentCaptor.forClass(NotificationDto.class);
        verify(messagingTemplate).convertAndSendToUser(
            ArgumentCaptor.forClass(String.class).capture(),
            ArgumentCaptor.forClass(String.class).capture(),
            notificationCaptor.capture()
        );

        NotificationDto capturedNotification = notificationCaptor.getValue();
        assertEquals(transaction.getId(), capturedNotification.getTransactionId());
        assertEquals(TransactionStatus.FAILED, capturedNotification.getStatus());
        assertEquals("Withdraw failed", capturedNotification.getMessage());
        assertEquals(errorMessage, capturedNotification.getDetails());
    }
}
