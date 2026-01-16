package com.shophub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartDTO {
    
    @JsonProperty("cart_id")
    private Integer cartId;
    
    @JsonProperty("user_id")
    private String userId;
    
    //@NotNull(message = "Product ID is required")
    @JsonProperty("product_id")
    private Integer productId;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    @JsonProperty("unit_price")
    private BigDecimal unitPrice;
    
    @JsonProperty("product_name")
    private String productName;
    
    private Object images;
}