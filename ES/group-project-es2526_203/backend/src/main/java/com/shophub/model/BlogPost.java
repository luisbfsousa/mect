package com.shophub.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "blog_posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogPost {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(name = "image_url")
    private String imageUrl;
    
    private String theme;
    
    @Column(nullable = false)
    private String status; // "draft" or "published"
    
    @Column(name = "markdown_content", nullable = false, columnDefinition = "TEXT")
    private String markdownContent;
    
    @Column(name = "author_id", nullable = false)
    private String authorId;
    
    @Column(name = "author_name", nullable = false)
    private String authorName;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
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