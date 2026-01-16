package com.shophub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackingSlipDTO {
    
    @JsonProperty("order_id")
    private Integer orderId;
    
    @JsonProperty("order_status")
    private String orderStatus;
    
    @JsonProperty("user_id")
    private String userId;
    
    private String email;
    
    @JsonProperty("first_name")
    private String firstName;
    
    @JsonProperty("last_name")
    private String lastName;
    
    @JsonProperty("shipping_address")
    private Map<String, Object> shippingAddress;
    
    @JsonProperty("tracking_number")
    private String trackingNumber;
    
    @JsonProperty("shipping_provider")
    private String shippingProvider;
    
    @JsonProperty("total_amount")
    private BigDecimal totalAmount;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;
    
    private List<PackingSlipItemDTO> items;
    
    @JsonProperty("item_count")
    private Integer itemCount;
    
    @JsonProperty("total_quantity")
    private Integer totalQuantity;
}
