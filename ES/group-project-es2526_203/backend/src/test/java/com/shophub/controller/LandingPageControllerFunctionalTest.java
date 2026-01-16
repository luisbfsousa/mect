package com.shophub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shophub.config.TestSecurityConfig;
import com.shophub.dto.LandingPageDTO;
import com.shophub.model.LandingPage;
import com.shophub.repository.LandingPageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
public class LandingPageControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LandingPageRepository landingPageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private LandingPage publishedPage;
    private LandingPage draftPage;

    @BeforeEach
    void setUp() throws Exception {
        landingPageRepository.deleteAll();

        publishedPage = LandingPage.builder()
                .title("Published Landing Page")
                .description("This page is published")
                .metadata(objectMapper.readTree("{\"hero\":\"Welcome\"}"))
                .isPublished(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        publishedPage = landingPageRepository.save(publishedPage);

        draftPage = LandingPage.builder()
                .title("Draft Landing Page")
                .description("This page is a draft")
                .metadata(objectMapper.readTree("{\"status\":\"draft\"}"))
                .isPublished(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        draftPage = landingPageRepository.save(draftPage);
    }

    @AfterEach
    void tearDown() {
        landingPageRepository.deleteAll();
    }

    @Test
    void getAllLandingPages_ReturnsAllPages() throws Exception {
        mockMvc.perform(get("/api/v1/landing-pages")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].title", containsInAnyOrder(
                    "Published Landing Page", 
                    "Draft Landing Page"
                )));
    }

    @Test
    void getPublishedLandingPages_ReturnsOnlyPublished() throws Exception {
        mockMvc.perform(get("/api/v1/landing-pages/published")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Published Landing Page")))
                .andExpect(jsonPath("$[0].isPublished", is(true)));
    }

    @Test
    void getLandingPageById_ReturnsPage() throws Exception {
        mockMvc.perform(get("/api/v1/landing-pages/" + publishedPage.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(publishedPage.getId().intValue())))
                .andExpect(jsonPath("$.title", is("Published Landing Page")))
                .andExpect(jsonPath("$.description", is("This page is published")))
                .andExpect(jsonPath("$.metadata").exists());
    }

    @Test
    @WithMockUser(roles = "content_manager")
    @Transactional
    void createLandingPage_WithContentManagerRole_CreatesPage() throws Exception {
        LandingPageDTO newPage = LandingPageDTO.builder()
                .title("New Landing Page")
                .description("Newly created page")
                .metadata("{\"section\":\"features\"}")
                .build();

        mockMvc.perform(post("/api/v1/landing-pages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newPage)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("New Landing Page")))
                .andExpect(jsonPath("$.description", is("Newly created page")))
                .andExpect(jsonPath("$.metadata", is("{\"section\":\"features\"}")))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    @WithMockUser(roles = "user")
    void createLandingPage_WithoutContentManagerRole_ReturnsForbidden() throws Exception {
        LandingPageDTO newPage = LandingPageDTO.builder()
                .title("Unauthorized Page")
                .description("Should not be created")
                .build();

        mockMvc.perform(post("/api/v1/landing-pages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newPage)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "content_manager")
    @Transactional
    void updateLandingPage_WithContentManagerRole_UpdatesPage() throws Exception {
        LandingPageDTO updateDto = LandingPageDTO.builder()
                .title("Updated Title")
                .description("Updated Description")
                .metadata("{\"updated\":true}")
                .build();

        mockMvc.perform(put("/api/v1/landing-pages/" + draftPage.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(draftPage.getId().intValue())))
                .andExpect(jsonPath("$.title", is("Updated Title")))
                .andExpect(jsonPath("$.description", is("Updated Description")))
                .andExpect(jsonPath("$.metadata", is("{\"updated\":true}")));
    }

    @Test
    @WithMockUser(roles = "content_manager")
    @Transactional
    void publishLandingPage_ChangesPublishedStatus() throws Exception {
        mockMvc.perform(put("/api/v1/landing-pages/" + draftPage.getId() + "/publish")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublished", is(true)));
    }

    @Test
    @WithMockUser(roles = "content_manager")
    @Transactional
    void unpublishLandingPage_ChangesPublishedStatus() throws Exception {
        mockMvc.perform(put("/api/v1/landing-pages/" + publishedPage.getId() + "/unpublish")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublished", is(false)));
    }

    @Test
    @WithMockUser(roles = "content_manager")
    @Transactional
    void deleteLandingPage_WithContentManagerRole_DeletesPage() throws Exception {
        Long pageId = draftPage.getId();

        mockMvc.perform(delete("/api/v1/landing-pages/" + pageId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Verify it's deleted
        mockMvc.perform(get("/api/v1/landing-pages/" + pageId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "user")
    void deleteLandingPage_WithoutContentManagerRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/landing-pages/" + draftPage.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void getLandingPageById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/landing-pages/99999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "content_manager")
    void updateLandingPage_NotFound_Returns404() throws Exception {
        LandingPageDTO updateDto = LandingPageDTO.builder()
                .title("Non-existent")
                .description("Does not exist")
                .build();

        mockMvc.perform(put("/api/v1/landing-pages/99999")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void publishedPagesEndpoint_FiltersCorrectly() throws Exception {
        // Create additional pages
        LandingPage anotherPublished = LandingPage.builder()
                .title("Another Published")
                .description("Also published")
                .isPublished(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        landingPageRepository.save(anotherPublished);

        LandingPage anotherDraft = LandingPage.builder()
                .title("Another Draft")
                .description("Still a draft")
                .isPublished(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        landingPageRepository.save(anotherDraft);

        // Verify only published pages are returned
        mockMvc.perform(get("/api/v1/landing-pages/published")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].isPublished", everyItem(is(true))));
    }
}
