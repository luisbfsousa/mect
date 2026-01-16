package com.shophub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostDTO {
    
    private Integer id;
    private String title;
    
    @JsonProperty("image_url")
    private String imageUrl;
    
    private String theme;
    private String status;
    
    @JsonProperty("markdown_content")
    private String markdownContent;
    
    @JsonProperty("author_id")
    private String authorId;
    
    @JsonProperty("author_name")
    private String authorName;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}