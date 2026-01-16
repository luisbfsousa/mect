package com.shophub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCustomerAddressesRequest {
    
    @NotNull(message = "Shipping info is required")
    @Valid
    @JsonProperty("shipping_info")
    private AddressInfo shippingInfo;
    
    @NotNull(message = "Billing info is required")
    @Valid
    @JsonProperty("billing_info")
    private AddressInfo billingInfo;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddressInfo {
        @NotNull(message = "Full name is required")
        private String fullName;
        
        @NotNull(message = "Address is required")
        private String address;
        
        @NotNull(message = "City is required")
        private String city;
        
        @NotNull(message = "Postal code is required")
        private String postalCode;
        
        @NotNull(message = "Phone is required")
        private String phone;
    }
}
