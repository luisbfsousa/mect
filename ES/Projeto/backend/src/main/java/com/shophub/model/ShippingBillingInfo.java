package com.shophub.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipping_billing_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingBillingInfo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, unique = true, length = 255)
    @JsonProperty("user_id")
    private String userId;
    
    // Shipping Information
    @Column(name = "shipping_full_name", length = 255)
    @JsonProperty("shipping_full_name")
    private String shippingFullName;
    
    @Column(name = "shipping_address", length = 500)
    @JsonProperty("shipping_address")
    private String shippingAddress;
    
    @Column(name = "shipping_city", length = 255)
    @JsonProperty("shipping_city")
    private String shippingCity;
    
    @Column(name = "shipping_postal_code", length = 20)
    @JsonProperty("shipping_postal_code")
    private String shippingPostalCode;
    
    @Column(name = "shipping_phone", length = 50)
    @JsonProperty("shipping_phone")
    private String shippingPhone;
    
    // Billing Information
    @Column(name = "billing_full_name", length = 255)
    @JsonProperty("billing_full_name")
    private String billingFullName;
    
    @Column(name = "billing_address", length = 500)
    @JsonProperty("billing_address")
    private String billingAddress;
    
    @Column(name = "billing_city", length = 255)
    @JsonProperty("billing_city")
    private String billingCity;
    
    @Column(name = "billing_postal_code", length = 20)
    @JsonProperty("billing_postal_code")
    private String billingPostalCode;
    
    @Column(name = "billing_phone", length = 50)
    @JsonProperty("billing_phone")
    private String billingPhone;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
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