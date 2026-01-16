package com.shophub.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    @JsonProperty("order_item_id")
    private Integer orderItemId;
    
    @Column(name = "order_id", nullable = false)
    @JsonProperty("order_id")
    private Integer orderId;
    
    @Column(name = "product_id", nullable = false)
    @JsonProperty("product_id")
    private Integer productId;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    @JsonProperty("unit_price")
    private BigDecimal unitPrice;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;
    
    @Transient
    @JsonProperty("product_name")
    private String productName;
    
    @Transient
    private Object images;
}
