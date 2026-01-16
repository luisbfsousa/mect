package com.shophub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {
    
    @JsonProperty("order_id")
    private Integer orderId;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("order_status")
    private String orderStatus;
    
    @JsonProperty("total_amount")
    private BigDecimal totalAmount;
    
    @JsonProperty("tax_amount")
    private BigDecimal taxAmount;
    
    @JsonProperty("shipping_cost")
    private BigDecimal shippingCost;
    
    @JsonProperty("shipping_address")
    private Object shippingAddress;
    
    @JsonProperty("billing_address")
    private Object billingAddress;
    
    @JsonProperty("tracking_number")
    private String trackingNumber;
    
    @JsonProperty("shipping_provider")
    private String shippingProvider;
    
    @JsonProperty("estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    private List<OrderItemDTO> items;
    
    private String email;
    
    @JsonProperty("first_name")
    private String firstName;
    
    @JsonProperty("last_name")
    private String lastName;
}
