package com.shophub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingBillingDTO {
    private AddressInfo shipping;
    private AddressInfo billing;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddressInfo {
        @JsonProperty("fullName")
        private String fullName;
        
        @JsonProperty("address")
        private String address;
        
        @JsonProperty("city")
        private String city;
        
        @JsonProperty("postalCode")
        private String postalCode;
        
        @JsonProperty("phone")
        private String phone;
    }
}