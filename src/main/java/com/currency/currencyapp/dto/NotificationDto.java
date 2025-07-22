package com.currency.currencyapp.dto;

import com.currency.currencyapp.domain.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private String transactionId;
    private TransactionStatus status;
    private String message;
    private String details;
}
