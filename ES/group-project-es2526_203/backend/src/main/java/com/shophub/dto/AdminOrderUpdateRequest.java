package com.shophub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrderUpdateRequest {
    
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "pending|processing|shipped|delivered|cancelled", 
             message = "Invalid order status")
    private String status;
    
    @JsonProperty("payment_confirmed")
    private Boolean paymentConfirmed;
    
    @JsonProperty("tracking_number")
    @Size(max = 100, message = "Tracking number must not exceed 100 characters")
    private String trackingNumber;
    
    @JsonProperty("shipping_provider")
    @Size(max = 100, message = "Shipping provider must not exceed 100 characters")
    private String shippingProvider;
    
    @JsonProperty("admin_notes")
    @Size(max = 500, message = "Admin notes must not exceed 500 characters")
    private String adminNotes;
}
