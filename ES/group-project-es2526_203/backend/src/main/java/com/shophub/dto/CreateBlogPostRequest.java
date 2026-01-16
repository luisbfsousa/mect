package com.shophub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBlogPostRequest {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @JsonProperty("image_url")
    private String imageUrl;
    
    private String theme;
    
    @NotBlank(message = "Status is required")
    private String status; // "draft" or "published"
    
    @NotBlank(message = "Content is required")
    @JsonProperty("markdown_content")
    private String markdownContent;
}