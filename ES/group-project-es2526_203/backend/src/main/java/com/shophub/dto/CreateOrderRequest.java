package com.shophub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {
    
    @NotNull(message = "Items are required")
    @Size(min = 1, message = "Order must contain at least one item")
    private List<OrderItemRequest> items;
    
    @NotNull(message = "Total is required")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal total;
    
    @NotNull(message = "Shipping information is required")
    private ShippingInfo shipping;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        @JsonProperty("product_id")
        private Integer productId;
        
        private Integer id;
        
        private Integer quantity;
        
        private BigDecimal price;
        
        @JsonProperty("product_name")
        private String productName;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingInfo {
        @NotNull(message = "Shipping address is required")
        private Address address;
        
        private BigDecimal cost;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        private String fullName;
        private String address;
        private String city;
        private String postalCode;
        private String phone;
    }
}