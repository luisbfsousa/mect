package com.shophub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBlogPostRequest {
    
    private String title;
    
    @JsonProperty("image_url")
    private String imageUrl;
    
    private String theme;
    private String status;
    
    @JsonProperty("markdown_content")
    private String markdownContent;
}