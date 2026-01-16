package com.shophub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shophub.config.TestSecurityConfig;
import com.shophub.dto.BannerDTO;
import com.shophub.model.Banner;
import com.shophub.repository.BannerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class BannerControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BannerRepository bannerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void cleanup() {
        bannerRepository.deleteAll();
    }

    private Banner createBanner(String title, boolean published, LocalDateTime start, LocalDateTime end, Integer priority) {
        return Banner.builder()
                .title(title)
                .description("Description for " + title)
                .imageUrl("https://example.com/" + title.toLowerCase().replace(" ", "-") + ".jpg")
                .isPublished(published)
                .startAt(start)
                .endAt(end)
                .priority(priority)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @Transactional
    void getActiveBanners_returnsOnlyCurrentlyRunning() throws Exception {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        bannerRepository.save(createBanner("Active Banner", true, 
                now.minusDays(1), now.plusDays(1), 10));
        bannerRepository.save(createBanner("Future Banner", true, 
                now.plusDays(1), now.plusDays(5), 5));
        bannerRepository.save(createBanner("Expired Banner", true, 
                now.minusDays(5), now.minusDays(1), 15));
        bannerRepository.save(createBanner("Unpublished Banner", false, 
                now.minusDays(1), now.plusDays(1), 20));

        // Act and Assert
        mockMvc.perform(get("/api/v1/banners/active")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Active Banner")));
    }

    @Test
    @Transactional
    void getActiveBanners_includesBannerWithNoDates() throws Exception {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        bannerRepository.save(createBanner("Always Active", true, null, null, 5));
        bannerRepository.save(createBanner("Active Now", true, 
                now.minusDays(1), now.plusDays(1), 10));
        bannerRepository.save(createBanner("Expired", true, 
                now.minusDays(5), now.minusDays(1), 8));

        // Act and Assert
        mockMvc.perform(get("/api/v1/banners/active")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("Active Now", "Always Active")));
    }

    @Test
    @Transactional
    void getActiveBanners_includesBannerWithOnlyStartDate() throws Exception {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        bannerRepository.save(createBanner("Started", true, 
                now.minusDays(1), null, 10));
        bannerRepository.save(createBanner("Not Started", true, 
                now.plusDays(1), null, 5));

        // Act and Assert
        mockMvc.perform(get("/api/v1/banners/active")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Started")));
    }

    @Test
    @Transactional
    void getActiveBanners_sortsByPriorityDescending() throws Exception {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        bannerRepository.save(createBanner("Low Priority", true, 
                now.minusDays(1), now.plusDays(1), 5));
        bannerRepository.save(createBanner("High Priority", true, 
                now.minusDays(1), now.plusDays(1), 20));
        bannerRepository.save(createBanner("Medium Priority", true, 
                now.minusDays(1), now.plusDays(1), 10));

        // Act and Assert
        mockMvc.perform(get("/api/v1/banners/active")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].title", is("High Priority")))
                .andExpect(jsonPath("$[1].title", is("Medium Priority")))
                .andExpect(jsonPath("$[2].title", is("Low Priority")));
    }

    @Test
    @Transactional
    void getActiveBanners_returnsCorrectFieldsInResponse() throws Exception {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startAt = now.minusDays(1);
        LocalDateTime endAt = now.plusDays(1);

        bannerRepository.save(Banner.builder()
                .title("Test Banner")
                .description("Test Description")
                .imageUrl("https://example.com/test.jpg")
                .isPublished(true)
                .startAt(startAt)
                .endAt(endAt)
                .priority(10)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        // Act and Assert
        mockMvc.perform(get("/api/v1/banners/active")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].title", is("Test Banner")))
                .andExpect(jsonPath("$[0].description", is("Test Description")))
                .andExpect(jsonPath("$[0].imageUrl", is("https://example.com/test.jpg")))
                .andExpect(jsonPath("$[0].published", is(true)))
                .andExpect(jsonPath("$[0].priority", is(10)))
                .andExpect(jsonPath("$[0].startAt").exists())
                .andExpect(jsonPath("$[0].endAt").exists());
    }

    @Test
    @Transactional
    void getActiveBanners_isPublicEndpoint() throws Exception {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        bannerRepository.save(createBanner("Active", true, 
                now.minusDays(1), now.plusDays(1), 10));

        // Act and Assert - No authentication required
        mockMvc.perform(get("/api/v1/banners/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @Transactional
    @WithMockUser(roles = "content_manager")
    void createBanner_withDates_createsSuccessfully() throws Exception {
        // Arrange
        LocalDateTime startAt = LocalDateTime.now().plusDays(1);
        LocalDateTime endAt = LocalDateTime.now().plusDays(10);

        BannerDTO dto = BannerDTO.builder()
                .title("Future Campaign")
                .description("Upcoming promotional banner")
                .imageUrl("https://example.com/future.jpg")
                .isPublished(true)
                .startAt(startAt.toString())
                .endAt(endAt.toString())
                .priority(10)
                .build();

        // Act and Assert
        mockMvc.perform(post("/api/v1/banners").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title", is("Future Campaign")))
                .andExpect(jsonPath("$.startAt").exists())
                .andExpect(jsonPath("$.endAt").exists())
                .andExpect(jsonPath("$.priority", is(10)));
    }

    @Test
    @Transactional
    @WithMockUser(roles = "content_manager")
    void updateBanner_canUpdateDates() throws Exception {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Banner banner = bannerRepository.save(createBanner("Original", true, 
                now.minusDays(1), now.plusDays(1), 10));

        LocalDateTime newStartAt = now.minusDays(2);
        LocalDateTime newEndAt = now.plusDays(5);

        BannerDTO updateDTO = BannerDTO.builder()
                .title("Original")
                .description("Description for Original")
                .imageUrl("https://example.com/original.jpg")
                .isPublished(true)
                .startAt(newStartAt.toString())
                .endAt(newEndAt.toString())
                .priority(10)
                .build();

        // Act and Assert
        mockMvc.perform(put("/api/v1/banners/{id}", banner.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(banner.getId().intValue())))
                .andExpect(jsonPath("$.startAt").exists())
                .andExpect(jsonPath("$.endAt").exists());
    }

    @Test
    @Transactional
    @WithMockUser(roles = "content_manager")
    void publishBanner_makesItActive() throws Exception {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Banner banner = bannerRepository.save(createBanner("Unpublished", false, 
                now.minusDays(1), now.plusDays(1), 10));

        // Act - Publish the banner
        mockMvc.perform(put("/api/v1/banners/{id}/publish", banner.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published", is(true)));

        // Assert - Should now appear in active banners
        mockMvc.perform(get("/api/v1/banners/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Unpublished")));
    }

    @Test
    @Transactional
    @WithMockUser(roles = "content_manager")
    void getAllBanners_returnsAllBanners_regardlessOfDates() throws Exception {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        bannerRepository.save(createBanner("Active", true, 
                now.minusDays(1), now.plusDays(1), 10));
        bannerRepository.save(createBanner("Future", true, 
                now.plusDays(1), now.plusDays(5), 5));
        bannerRepository.save(createBanner("Expired", true, 
                now.minusDays(5), now.minusDays(1), 8));
        bannerRepository.save(createBanner("Unpublished", false, 
                now.minusDays(1), now.plusDays(1), 15));

        // Act and Assert - Admin endpoint returns all banners
        mockMvc.perform(get("/api/v1/banners")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].title", 
                        containsInAnyOrder("Active", "Future", "Expired", "Unpublished")));
    }

    @Test
    @Transactional
    @WithMockUser(roles = "user")
    void createBanner_withoutContentManagerRole_returnsForbidden() throws Exception {
        // Arrange
        BannerDTO dto = BannerDTO.builder()
                .title("Test Banner")
                .description("Test")
                .imageUrl("https://example.com/test.jpg")
                .isPublished(true)
                .priority(10)
                .build();

        // Act and Assert
        mockMvc.perform(post("/api/v1/banners").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @WithMockUser(roles = "user")
    void publishBanner_withoutContentManagerRole_returnsForbidden() throws Exception {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Banner banner = bannerRepository.save(createBanner("Test", false, 
                now.minusDays(1), now.plusDays(1), 10));

        // Act and Assert
        mockMvc.perform(put("/api/v1/banners/{id}/publish", banner.getId()).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
