package com.shophub.dto;

public class ChatRequest {
    private String query;
    private String sessionId;
    private String userId;

    public ChatRequest() {
    }

    public ChatRequest(String query, String sessionId) {
        this.query = query;
        this.sessionId = sessionId;
    }

    // Getters and Setters
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
