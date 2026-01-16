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
public class LandingPageDTO {
    private Long id;
    private String title;
    private String description;
    private String metadata;

    private String startDate;
    private String endDate;

    @JsonProperty("isPublished")
    private boolean published;

    @JsonProperty("isBanner")
    private boolean banner;
}
