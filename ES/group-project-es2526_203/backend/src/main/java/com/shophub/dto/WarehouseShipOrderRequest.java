package com.shophub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WarehouseShipOrderRequest {
    
    @JsonProperty("tracking_number")
    @Size(max = 100, message = "Tracking number must be at most 100 characters")
    private String trackingNumber;
    
    @JsonProperty("shipping_provider")
    @Size(max = 100, message = "Shipping provider must be at most 100 characters")
    private String shippingProvider;
}
