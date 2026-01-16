package com.shophub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerDTO {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private String metadata;
    private String startAt; // ISO string
    private String endAt; // ISO string
    private boolean isPublished;
    private Integer priority;
    private Long landingPageId;
}
