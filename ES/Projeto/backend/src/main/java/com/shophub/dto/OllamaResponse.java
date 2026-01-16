package com.shophub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OllamaResponse {
    private String model;

    @JsonProperty("created_at")
    private String createdAt;

    private String response;

    private boolean done;

    @JsonProperty("total_duration")
    private Long totalDuration;

    @JsonProperty("prompt_eval_count")
    private Integer promptEvalCount;

    @JsonProperty("eval_count")
    private Integer evalCount;

    public OllamaResponse() {
    }

    // Getters and Setters
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public Long getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(Long totalDuration) {
        this.totalDuration = totalDuration;
    }

    public Integer getPromptEvalCount() {
        return promptEvalCount;
    }

    public void setPromptEvalCount(Integer promptEvalCount) {
        this.promptEvalCount = promptEvalCount;
    }

    public Integer getEvalCount() {
        return evalCount;
    }

    public void setEvalCount(Integer evalCount) {
        this.evalCount = evalCount;
    }

    public Integer getTotalTokens() {
        return (promptEvalCount != null ? promptEvalCount : 0) +
               (evalCount != null ? evalCount : 0);
    }
}
