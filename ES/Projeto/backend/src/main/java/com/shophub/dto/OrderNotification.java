package com.shophub.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderNotification {
    
    private Integer orderId;
    private String userId;
    private String customerEmail;
    private String customerName;
    private String oldStatus;
    private String newStatus;
    private String trackingNumber;
    private String shippingProvider;
    private BigDecimal totalAmount;
    private LocalDateTime updatedAt;
    private String message;
}
