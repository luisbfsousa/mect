package com.shophub.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    @JsonProperty("order_id")
    private Integer orderId;
    
    @Column(name = "user_id", nullable = false)
    @JsonProperty("user_id")
    private String userId;
    
    @Column(name = "order_status", nullable = false)
    @JsonProperty("order_status")
    private String orderStatus;
    
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    @JsonProperty("total_amount")
    private BigDecimal totalAmount;
    
    @Column(name = "tax_amount", precision = 10, scale = 2)
    @JsonProperty("tax_amount")
    private BigDecimal taxAmount;
    
    @Column(name = "shipping_cost", precision = 10, scale = 2)
    @JsonProperty("shipping_cost")
    private BigDecimal shippingCost;
    
    @Column(name = "shipping_address", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @JsonProperty("shipping_address")
    private Map<String, Object> shippingAddress;
    
    @Column(name = "billing_address", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @JsonProperty("billing_address")
    private Map<String, Object> billingAddress;
    
    @Column(name = "tracking_number")
    @JsonProperty("tracking_number")
    private String trackingNumber;
    
    @Column(name = "shipping_provider")
    @JsonProperty("shipping_provider")
    private String shippingProvider;
    
    @Column(name = "estimated_delivery_date")
    @JsonProperty("estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    @Transient
    private List<OrderItem> items;
    
    @Transient
    private String email;
    
    @Transient
    @JsonProperty("first_name")
    private String firstName;
    
    @Transient
    @JsonProperty("last_name")
    private String lastName;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}