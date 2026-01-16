package com.shophub.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    @JsonProperty("review_id")
    private Integer reviewId;
    
    @Column(name = "product_id", nullable = false)
    @JsonProperty("product_id")
    private Integer productId;
    
    @Column(name = "user_id")
    @JsonProperty("user_id")
    private String userId;
    
    @Column(name = "rating", nullable = false)
    private Integer rating;
    
    @Column(name = "title", nullable = false)
    private String title;
    
    @Column(name = "comment", nullable = false, columnDefinition = "TEXT")
    private String comment;
    
    @Column(name = "user_name", nullable = false)
    @JsonProperty("user_name")
    private String userName;
    
    @Column(name = "verified_purchase")
    @JsonProperty("verified_purchase")
    private Boolean verifiedPurchase = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
