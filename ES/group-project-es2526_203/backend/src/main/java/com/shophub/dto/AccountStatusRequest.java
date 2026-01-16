package com.shophub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountStatusRequest {
    
    @JsonProperty("locked")
    private Boolean locked;
    
    @JsonProperty("deactivated")
    private Boolean deactivated;
    
    @JsonProperty("reason")
    private String reason;
}
