package com.shophub.model;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chatbot_prompts")
public class ChatbotPrompt {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String version;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String template;

    @Column(nullable = false)
    private Boolean active = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public ChatbotPrompt() {
        this.createdAt = LocalDateTime.now();
    }

    public ChatbotPrompt(String version, String template, Boolean active) {
        this();
        this.version = version;
        this.template = template;
        this.active = active;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
