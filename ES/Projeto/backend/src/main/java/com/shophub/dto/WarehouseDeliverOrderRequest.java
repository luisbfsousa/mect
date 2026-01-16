package com.shophub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WarehouseDeliverOrderRequest {
    
    @NotNull(message = "Delivery confirmation is required")
    @JsonProperty("confirm")
    private Boolean confirm;
}
