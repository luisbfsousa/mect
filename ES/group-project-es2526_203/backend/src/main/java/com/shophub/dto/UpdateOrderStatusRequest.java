package com.shophub.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {
    
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "pending|processing|shipped|delivered|cancelled", 
             message = "Invalid order status")
    private String status;
}