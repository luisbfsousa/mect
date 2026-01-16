package com.shophub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDTO {
    
    private Long id;
    
    @JsonProperty("admin_user_id")
    private String adminUserId;
    
    @JsonProperty("target_user_id")
    private String targetUserId;
    
    private String action;
    
    private Map<String, Object> details;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
