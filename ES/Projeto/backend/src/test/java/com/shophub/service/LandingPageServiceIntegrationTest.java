package com.shophub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shophub.dto.LandingPageDTO;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.model.LandingPage;
import com.shophub.repository.LandingPageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
public class LandingPageServiceIntegrationTest {

    @Autowired
    private LandingPageService landingPageService;

    @Autowired
    private LandingPageRepository landingPageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void cleanup() {
        landingPageRepository.deleteAll();
    }

    @Test
    @Transactional
    void createLandingPage_savesSuccessfully() throws Exception {
        // Arrange
        LandingPageDTO dto = LandingPageDTO.builder()
                .title("Integration Test Landing Page")
                .description("Testing end-to-end creation")
                .metadata("{\"hero\":\"Welcome\",\"cta\":\"Shop Now\"}")
                .published(false)
                .build();

        // Act
        LandingPageDTO created = landingPageService.createLandingPage(dto);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getTitle()).isEqualTo("Integration Test Landing Page");
        assertThat(created.isPublished()).isFalse();

        // Verify in database
        LandingPage fromDb = landingPageRepository.findById(created.getId()).orElseThrow();
        assertThat(fromDb.getTitle()).isEqualTo("Integration Test Landing Page");
    }

    @Test
    @Transactional
    void createLandingPage_withNullMetadata_savesSuccessfully() {
        // Arrange
        LandingPageDTO dto = LandingPageDTO.builder()
                .title("Page Without Metadata")
                .description("No metadata provided")
                .metadata(null)
                .published(false)
                .build();

        // Act
        LandingPageDTO created = landingPageService.createLandingPage(dto);

        // Assert
        assertThat(created.getMetadata()).isNull();
        
        LandingPage fromDb = landingPageRepository.findById(created.getId()).orElseThrow();
        assertThat(fromDb.getMetadata()).isNull();
    }

    @Test
    @Transactional
    void updateLandingPage_updatesFieldsSuccessfully() throws Exception {
        // Arrange
        LandingPage page = LandingPage.builder()
                .title("Original Title")
                .description("Original Description")
                .metadata(objectMapper.readTree("{\"key\":\"value\"}"))
                .isPublished(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        page = landingPageRepository.save(page);

        LandingPageDTO updateDTO = LandingPageDTO.builder()
                .title("Updated Title")
                .description("Updated Description")
                .build();

        // Act
        LandingPageDTO updated = landingPageService.updateLandingPage(page.getId(), updateDTO);

        // Assert
        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getDescription()).isEqualTo("Updated Description");
    }

    @Test
    @Transactional
    void getLandingPage_retrievesPageById() throws Exception {
        // Arrange
        LandingPage page = LandingPage.builder()
                .title("Test Page")
                .description("Test Description")
                .isPublished(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        page = landingPageRepository.save(page);

        // Act
        LandingPageDTO retrieved = landingPageService.getLandingPage(page.getId());

        // Assert
        assertThat(retrieved.getId()).isEqualTo(page.getId());
        assertThat(retrieved.getTitle()).isEqualTo("Test Page");
    }

    @Test
    @Transactional
    void getLandingPage_throwsExceptionWhenNotFound() {
        // Act & Assert
        assertThatThrownBy(() -> landingPageService.getLandingPage(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Landing page not found");
    }

    @Test
    @Transactional
    void getAllLandingPages_returnsAllPages() {
        // Arrange
        landingPageRepository.save(createTestPage("Page 1", true));
        landingPageRepository.save(createTestPage("Page 2", false));
        landingPageRepository.save(createTestPage("Page 3", true));

        // Act
        List<LandingPageDTO> allPages = landingPageService.getAllLandingPages();

        // Assert
        assertThat(allPages).hasSize(3);
        assertThat(allPages).extracting("title")
                .containsExactlyInAnyOrder("Page 1", "Page 2", "Page 3");
    }

    @Test
    @Transactional
    void getPublishedLandingPages_returnsOnlyPublishedPages() {
        // Arrange
        landingPageRepository.save(createTestPage("Published 1", true));
        landingPageRepository.save(createTestPage("Draft", false));
        landingPageRepository.save(createTestPage("Published 2", true));

        // Act
        List<LandingPageDTO> publishedPages = landingPageService.getPublishedLandingPages();

        // Assert
        assertThat(publishedPages).hasSize(2);
        assertThat(publishedPages).allMatch(page -> page.isPublished());
    }

    @Test
    @Transactional
    void publishLandingPage_setsPublishedFlagAndTimestamp() {
        // Arrange
        LandingPage page = landingPageRepository.save(createTestPage("Draft", false));

        // Act
        LandingPageDTO published = landingPageService.publishLandingPage(page.getId());

        // Assert
        assertThat(published.isPublished()).isTrue();
        
        LandingPage fromDb = landingPageRepository.findById(page.getId()).orElseThrow();
        assertThat(fromDb.isPublished()).isTrue();
        assertThat(fromDb.getPublishedAt()).isNotNull();
    }

    @Test
    @Transactional
    void unpublishLandingPage_clearsPublishedFlagAndTimestamp() {
        // Arrange
        LandingPage page = createTestPage("Published", true);
        page.setPublishedAt(LocalDateTime.now());
        page = landingPageRepository.save(page);

        // Act
        LandingPageDTO unpublished = landingPageService.unpublishLandingPage(page.getId());

        // Assert
        assertThat(unpublished.isPublished()).isFalse();
        
        LandingPage fromDb = landingPageRepository.findById(page.getId()).orElseThrow();
        assertThat(fromDb.isPublished()).isFalse();
        assertThat(fromDb.getPublishedAt()).isNull();
    }

    @Test
    @Transactional
    void deleteLandingPage_removesFromDatabase() {
        // Arrange
        LandingPage page = landingPageRepository.save(createTestPage("To Delete", false));
        Long pageId = page.getId();

        // Act
        landingPageService.deleteLandingPage(pageId);

        // Assert
        assertThat(landingPageRepository.existsById(pageId)).isFalse();
    }

    // Helper method
    private LandingPage createTestPage(String title, boolean published) {
        return LandingPage.builder()
                .title(title)
                .description("Test description for " + title)
                .isPublished(published)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}