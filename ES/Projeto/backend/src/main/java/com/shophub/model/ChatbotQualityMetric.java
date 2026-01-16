package com.shophub.model;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chatbot_quality_metrics")
public class ChatbotQualityMetric {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatbotMessage message;

    @Column(name = "relevance_score")
    private Float relevanceScore;

    @Column(name = "contains_product_info")
    private Boolean containsProductInfo;

    @Column(name = "requires_fallback")
    private Boolean requiresFallback;

    @Column(name = "user_feedback", length = 20)
    private String userFeedback; // 'helpful', 'not_helpful', 'inappropriate'

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public ChatbotQualityMetric() {
        this.createdAt = LocalDateTime.now();
    }

    public ChatbotQualityMetric(ChatbotMessage message, String userFeedback) {
        this();
        this.message = message;
        this.userFeedback = userFeedback;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ChatbotMessage getMessage() {
        return message;
    }

    public void setMessage(ChatbotMessage message) {
        this.message = message;
    }

    public Float getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(Float relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public Boolean getContainsProductInfo() {
        return containsProductInfo;
    }

    public void setContainsProductInfo(Boolean containsProductInfo) {
        this.containsProductInfo = containsProductInfo;
    }

    public Boolean getRequiresFallback() {
        return requiresFallback;
    }

    public void setRequiresFallback(Boolean requiresFallback) {
        this.requiresFallback = requiresFallback;
    }

    public String getUserFeedback() {
        return userFeedback;
    }

    public void setUserFeedback(String userFeedback) {
        this.userFeedback = userFeedback;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
