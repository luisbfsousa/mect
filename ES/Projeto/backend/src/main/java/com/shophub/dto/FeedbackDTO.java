package com.shophub.dto;

import com.shophub.model.ChatbotQualityMetric;

import java.time.LocalDateTime;

public class FeedbackDTO {
    private String id;
    private String messageId;
    private String messageContent;
    private String userFeedback;
    private Float relevanceScore;
    private Boolean containsProductInfo;
    private Boolean requiresFallback;
    private LocalDateTime createdAt;

    public FeedbackDTO() {
    }

    public FeedbackDTO(ChatbotQualityMetric metric) {
        this.id = metric.getId().toString();
        this.messageId = metric.getMessage().getId().toString();
        this.messageContent = metric.getMessage().getContent();
        this.userFeedback = metric.getUserFeedback();
        this.relevanceScore = metric.getRelevanceScore();
        this.containsProductInfo = metric.getContainsProductInfo();
        this.requiresFallback = metric.getRequiresFallback();
        this.createdAt = metric.getCreatedAt();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public String getUserFeedback() {
        return userFeedback;
    }

    public void setUserFeedback(String userFeedback) {
        this.userFeedback = userFeedback;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
