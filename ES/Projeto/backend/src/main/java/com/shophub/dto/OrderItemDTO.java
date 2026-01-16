package com.shophub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDTO {
    
    @JsonProperty("order_item_id")
    private Integer orderItemId;
    
    @JsonProperty("order_id")
    private Integer orderId;
    
    @JsonProperty("product_id")
    private Integer productId;
    
    @JsonProperty("product_name")
    private String productName;
    
    private Integer quantity;
    
    @JsonProperty("unit_price")
    private BigDecimal unitPrice;
    
    private BigDecimal subtotal;
    
    private Object images;
}
