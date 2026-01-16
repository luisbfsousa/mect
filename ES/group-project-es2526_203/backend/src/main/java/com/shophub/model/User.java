package com.shophub.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User {
    
    @Id
    @Column(name = "user_id", length = 255)
    @JsonProperty("user_id")
    private String userId;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(name = "first_name", length = 100)
    @JsonProperty("first_name")
    private String firstName;
    
    @Column(name = "last_name", length = 100)
    @JsonProperty("last_name")
    private String lastName;
    
    @Column(length = 20)
    private String phone;
    
    @Column(nullable = false, length = 50)
    private String role = "customer";
    
    @Column(name = "is_locked")
    @JsonProperty("is_locked")
    private Boolean isLocked = false;
    
    @Column(name = "is_deactivated")
    @JsonProperty("is_deactivated")
    private Boolean isDeactivated = false;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
