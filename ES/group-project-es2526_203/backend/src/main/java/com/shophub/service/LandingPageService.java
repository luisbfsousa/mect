package com.shophub.service;

import com.shophub.dto.LandingPageDTO;
import com.shophub.model.LandingPage;
import com.shophub.repository.LandingPageRepository;
import com.shophub.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.shophub.exception.InvalidInputException;

@Service
@RequiredArgsConstructor
public class LandingPageService {
    private final LandingPageRepository landingPageRepository;

    public LandingPageDTO createLandingPage(LandingPageDTO dto) {
        JsonNode metadataNode = parseMetadata(dto.getMetadata());

        LandingPage landingPage = LandingPage.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .metadata(metadataNode)
                .isPublished(false)
                .isBanner(dto.isBanner())
                .startDate(dto.getStartDate() != null ? LocalDateTime.parse(dto.getStartDate()) : null)
                .endDate(dto.getEndDate() != null ? LocalDateTime.parse(dto.getEndDate()) : null)
                .build();

        LandingPage saved = landingPageRepository.save(landingPage);
        return convertToDTO(saved);
    }

    public LandingPageDTO updateLandingPage(Long id, LandingPageDTO dto) {
        LandingPage landingPage = landingPageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Landing page not found with id: " + id));

        landingPage.setTitle(dto.getTitle());
        landingPage.setDescription(dto.getDescription());
        landingPage.setMetadata(parseMetadata(dto.getMetadata()));
        landingPage.setBanner(dto.isBanner());
        landingPage.setPublished(dto.isPublished());
        landingPage.setStartDate(dto.getStartDate() != null ? LocalDateTime.parse(dto.getStartDate()) : null);
        landingPage.setEndDate(dto.getEndDate() != null ? LocalDateTime.parse(dto.getEndDate()) : null);

        LandingPage updated = landingPageRepository.save(landingPage);
        return convertToDTO(updated);
    }

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

    public LandingPageDTO publishLandingPage(Long id) {
        LandingPage landingPage = landingPageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Landing page not found with id: " + id));

        landingPage.setPublished(true);
        landingPage.setPublishedAt(LocalDateTime.now());
        
        LandingPage published = landingPageRepository.save(landingPage);
        return convertToDTO(published);
    }

    public LandingPageDTO unpublishLandingPage(Long id) {
        LandingPage landingPage = landingPageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Landing page not found with id: " + id));

        landingPage.setPublished(false);
        landingPage.setPublishedAt(null);

        LandingPage updated = landingPageRepository.save(landingPage);
        return convertToDTO(updated);
    }

    public List<LandingPageDTO> getAllLandingPages() {
        return landingPageRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public LandingPageDTO getLandingPage(Long id) {
        LandingPage landingPage = landingPageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Landing page not found with id: " + id));
        return convertToDTO(landingPage);
    }

    public List<LandingPageDTO> getPublishedLandingPages() {
        LocalDateTime now = LocalDateTime.now();
        return landingPageRepository.findByIsPublishedTrue().stream()
            .filter(lp -> {
                if (lp.getStartDate() != null && now.isBefore(lp.getStartDate())) return false;
                if (lp.getEndDate() != null && now.isAfter(lp.getEndDate())) return false;
                return true;
            })
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public void deleteLandingPage(Long id) {
        if (!landingPageRepository.existsById(id)) {
            throw new ResourceNotFoundException("Landing page not found with id: " + id);
        }
        landingPageRepository.deleteById(id);
    }

    private LandingPageDTO convertToDTO(LandingPage landingPage) {
        return LandingPageDTO.builder()
                .id(landingPage.getId())
                .title(landingPage.getTitle())
                .description(landingPage.getDescription())
                .metadata(landingPage.getMetadata() != null ? landingPage.getMetadata().toString() : null)
                .startDate(landingPage.getStartDate() != null ? landingPage.getStartDate().toString() : null)
                .endDate(landingPage.getEndDate() != null ? landingPage.getEndDate().toString() : null)
                .published(landingPage.isPublished())
                .banner(landingPage.isBanner())
                .build();
    }
}
