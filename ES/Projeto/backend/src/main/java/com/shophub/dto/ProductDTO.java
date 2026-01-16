package com.shophub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {
    
    @JsonProperty("product_id")
    private Integer productId;
    
    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;
    
    private String description;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;
    
    private String sku;
    
    @JsonProperty("category_id")
    private Integer categoryId;
    
    @JsonProperty("category_name")
    private String categoryName;
    
    @Min(value = 0, message = "Stock quantity cannot be negative")
    @JsonProperty("stock_quantity")
    private Integer stockQuantity;
    
    @JsonProperty("low_stock_threshold")
    private Integer lowStockThreshold;
    
    private List<String> images;  // Changed to List<String>
    
    private Map<String, Object> specifications;  // Changed to Map
}