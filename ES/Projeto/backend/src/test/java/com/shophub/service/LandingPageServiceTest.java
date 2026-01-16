package com.shophub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shophub.dto.LandingPageDTO;
import com.shophub.exception.InvalidInputException;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.model.LandingPage;
import com.shophub.repository.LandingPageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LandingPageServiceTest {

    @Mock
    private LandingPageRepository landingPageRepository;

    @InjectMocks
    private LandingPageService landingPageService;

    private ObjectMapper objectMapper;
    private JsonNode metadataNode;
    private LandingPageDTO sampleDto;
    private LandingPage persistedEntity;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        metadataNode = objectMapper.readTree("{\"hero\":\"Welcome\"}");

        sampleDto = LandingPageDTO.builder()
                .title("Sample Landing Page")
                .description("Description")
                .metadata("{\"hero\":\"Welcome\"}")
                .published(false)
                .banner(false)
                .startDate(LocalDateTime.now().toString())
                .endDate(LocalDateTime.now().plusDays(1).toString())
                .build();

        persistedEntity = LandingPage.builder()
                .id(1L)
                .title(sampleDto.getTitle())
                .description(sampleDto.getDescription())
                .metadata(metadataNode)
                .isPublished(false)
                .startDate(LocalDateTime.parse(sampleDto.getStartDate()))
                .endDate(LocalDateTime.parse(sampleDto.getEndDate()))
                .build();
    }

    @Test
    void createLandingPage_savesAndReturnsDto() {
        when(landingPageRepository.save(any(LandingPage.class))).thenReturn(persistedEntity);

        LandingPageDTO result = landingPageService.createLandingPage(sampleDto);

        assertNotNull(result);
        assertEquals(persistedEntity.getId(), result.getId());
        verify(landingPageRepository).save(any(LandingPage.class));
    }

    @Test
    void createLandingPage_withInvalidJsonMetadata_throwsInvalidInputException() {
        sampleDto.setMetadata("{invalid json}");

        assertThrows(InvalidInputException.class,
                () -> landingPageService.createLandingPage(sampleDto));
        verify(landingPageRepository, never()).save(any());
    }

    @Test
    void getLandingPage_whenPresent_returnsDto() {
        when(landingPageRepository.findById(1L)).thenReturn(Optional.of(persistedEntity));

        LandingPageDTO result = landingPageService.getLandingPage(1L);

        assertEquals("Sample Landing Page", result.getTitle());
        verify(landingPageRepository).findById(1L);
    }

    @Test
    void getLandingPage_whenMissing_throwsResourceNotFound() {
        when(landingPageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> landingPageService.getLandingPage(1L));
    }

    @Test
    void getAllLandingPages_returnsConvertedDtos() {
        when(landingPageRepository.findAll()).thenReturn(List.of(persistedEntity));

        List<LandingPageDTO> result = landingPageService.getAllLandingPages();

        assertEquals(1, result.size());
        assertEquals("Sample Landing Page", result.get(0).getTitle());
    }

    @Test
    void updateLandingPage_updatesExistingEntity() {
        when(landingPageRepository.findById(1L)).thenReturn(Optional.of(persistedEntity));
        when(landingPageRepository.save(any(LandingPage.class))).thenReturn(persistedEntity);

        LandingPageDTO updateDto = LandingPageDTO.builder()
                .title("Updated")
                .description("Updated description")
                .metadata("{\"hero\":\"Updated\"}")
                .published(true)
                .banner(true)
                .build();

        LandingPageDTO result = landingPageService.updateLandingPage(1L, updateDto);

        assertEquals("Updated", result.getTitle());
        verify(landingPageRepository).save(any(LandingPage.class));
    }

    @Test
    void publishLandingPage_setsPublishedFlag() {
        when(landingPageRepository.findById(1L)).thenReturn(Optional.of(persistedEntity));

        LandingPage publishedEntity = cloneLandingPage(persistedEntity);
        publishedEntity.setPublished(true);
        when(landingPageRepository.save(any(LandingPage.class))).thenReturn(publishedEntity);

        LandingPageDTO result = landingPageService.publishLandingPage(1L);

        assertTrue(result.isPublished());
    }

    @Test
    void unpublishLandingPage_setsPublishedFalse() {
        LandingPage published = cloneLandingPage(persistedEntity);
        published.setPublished(true);
        published.setPublishedAt(LocalDateTime.now());

        when(landingPageRepository.findById(1L)).thenReturn(Optional.of(published));
        when(landingPageRepository.save(any(LandingPage.class))).thenReturn(persistedEntity);

        LandingPageDTO result = landingPageService.unpublishLandingPage(1L);

        assertFalse(result.isPublished());
    }

    @Test
    void deleteLandingPage_whenExists_deletes() {
        when(landingPageRepository.existsById(1L)).thenReturn(true);

        landingPageService.deleteLandingPage(1L);

        verify(landingPageRepository).deleteById(1L);
    }

    @Test
    void deleteLandingPage_whenMissing_throwsResourceNotFound() {
        when(landingPageRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> landingPageService.deleteLandingPage(1L));
    }

    @Test
    void getPublishedLandingPages_filtersByDates() {
        LandingPage active = LandingPage.builder()
                .id(1L)
                .title("Active")
                .description("desc")
                .metadata(metadataNode)
                .isPublished(true)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(1))
                .build();

        LandingPage expired = LandingPage.builder()
                .id(2L)
                .title("Expired")
                .isPublished(true)
                .startDate(LocalDateTime.now().minusDays(5))
                .endDate(LocalDateTime.now().minusDays(1))
                .build();

        when(landingPageRepository.findByIsPublishedTrue()).thenReturn(List.of(active, expired));

        List<LandingPageDTO> result = landingPageService.getPublishedLandingPages();

        assertEquals(1, result.size());
        assertEquals("Active", result.get(0).getTitle());
    }

    private LandingPage cloneLandingPage(LandingPage source) {
        return LandingPage.builder()
                .id(source.getId())
                .title(source.getTitle())
                .description(source.getDescription())
                .metadata(source.getMetadata())
                .createdAt(source.getCreatedAt())
                .updatedAt(source.getUpdatedAt())
                .publishedAt(source.getPublishedAt())
                .startDate(source.getStartDate())
                .endDate(source.getEndDate())
                .isPublished(source.isPublished())
                .isBanner(source.isBanner())
                .build();
    }
}
