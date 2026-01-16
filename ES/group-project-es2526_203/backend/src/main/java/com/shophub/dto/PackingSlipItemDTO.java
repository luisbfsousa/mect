package com.shophub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackingSlipItemDTO {
    
    @JsonProperty("product_name")
    private String productName;
    
    @JsonProperty("product_id")
    private Integer productId;
    
    private Integer quantity;
    
    @JsonProperty("unit_price")
    private BigDecimal unitPrice;
    
    private BigDecimal subtotal;
}
