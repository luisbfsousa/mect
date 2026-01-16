package com.shophub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.shophub.dto.BannerDTO;
import com.shophub.exception.InvalidInputException;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.model.Banner;
import com.shophub.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BannerService {
    private final BannerRepository bannerRepository;

    private JsonNode parseMetadata(String metadata) {
        if (metadata == null || metadata.trim().isEmpty()) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readTree(metadata);
        } catch (JsonProcessingException e) {
            throw new InvalidInputException("Invalid JSON for metadata: " + e.getOriginalMessage());
        }
    }

    public BannerDTO createBanner(BannerDTO dto) {
        Banner banner = Banner.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .metadata(parseMetadata(dto.getMetadata()))
                .startAt(dto.getStartAt() != null ? LocalDateTime.parse(dto.getStartAt()) : null)
                .endAt(dto.getEndAt() != null ? LocalDateTime.parse(dto.getEndAt()) : null)
                .isPublished(dto.isPublished())
                .priority(dto.getPriority() == null ? 0 : dto.getPriority())
                .landingPageId(dto.getLandingPageId())
                .build();

        Banner saved = bannerRepository.save(banner);
        return convertToDTO(saved);
    }

    public BannerDTO updateBanner(Long id, BannerDTO dto) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with id: " + id));

        banner.setTitle(dto.getTitle());
        banner.setDescription(dto.getDescription());
        banner.setImageUrl(dto.getImageUrl());
        banner.setMetadata(parseMetadata(dto.getMetadata()));
        banner.setStartAt(dto.getStartAt() != null ? LocalDateTime.parse(dto.getStartAt()) : null);
        banner.setEndAt(dto.getEndAt() != null ? LocalDateTime.parse(dto.getEndAt()) : null);
        banner.setPublished(dto.isPublished());
        banner.setPriority(dto.getPriority() == null ? 0 : dto.getPriority());
        banner.setLandingPageId(dto.getLandingPageId());

        Banner updated = bannerRepository.save(banner);
        return convertToDTO(updated);
    }

    public BannerDTO publishBanner(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with id: " + id));

        banner.setPublished(true);
        Banner published = bannerRepository.save(banner);
        return convertToDTO(published);
    }

    public List<BannerDTO> getAllBanners() {
        return bannerRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public BannerDTO getBanner(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with id: " + id));
        return convertToDTO(banner);
    }

    public List<BannerDTO> getActiveBanners() {
        // public read - no feature toggle check here because rendering is allowed for visitors
        LocalDateTime now = LocalDateTime.now();
        List<Banner> candidates = bannerRepository.findAll().stream()
                .filter(b -> b.isPublished())
                .filter(b -> (b.getStartAt() == null || !b.getStartAt().isAfter(now)))
                .filter(b -> (b.getEndAt() == null || !b.getEndAt().isBefore(now)))
                .sorted((a, b) -> Integer.compare(b.getPriority() == null ? 0 : b.getPriority(), a.getPriority() == null ? 0 : a.getPriority()))
                .collect(Collectors.toList());

        return candidates.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private BannerDTO convertToDTO(Banner banner) {
        return BannerDTO.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .description(banner.getDescription())
                .imageUrl(banner.getImageUrl())
                .metadata(banner.getMetadata() != null ? banner.getMetadata().toString() : null)
                .startAt(banner.getStartAt() != null ? banner.getStartAt().toString() : null)
                .endAt(banner.getEndAt() != null ? banner.getEndAt().toString() : null)
                .isPublished(banner.isPublished())
                .priority(banner.getPriority())
                .landingPageId(banner.getLandingPageId())
                .build();
    }
}
