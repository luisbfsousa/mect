package com.shophub.dto;

public class OllamaRequest {
    private String model;
    private String prompt;
    private boolean stream = false;

    public OllamaRequest() {
    }

    public OllamaRequest(String model, String prompt) {
        this.model = model;
        this.prompt = prompt;
    }

    // Getters and Setters
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }
}
