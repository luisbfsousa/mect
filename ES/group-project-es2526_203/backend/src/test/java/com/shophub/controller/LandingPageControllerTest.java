package com.shophub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shophub.dto.LandingPageDTO;
import com.shophub.service.LandingPageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LandingPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class LandingPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LandingPageService landingPageService;

    // Prevent JPA from loading real entities during slice test
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @WithMockUser(roles = "content_manager")
    void createLandingPage_Success() throws Exception {
        LandingPageDTO inputDto = LandingPageDTO.builder()
                .title("Test Landing Page")
                .description("Test Description")
                .metadata("{\"key\":\"value\"}")
                .build();

        LandingPageDTO outputDto = LandingPageDTO.builder()
                .id(1L)
                .title("Test Landing Page")
                .description("Test Description")
                .metadata("{\"key\":\"value\"}")
                .build();

        when(landingPageService.createLandingPage(any(LandingPageDTO.class))).thenReturn(outputDto);

        mockMvc.perform(post("/api/v1/landing-pages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Landing Page"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.metadata").value("{\"key\":\"value\"}"));
    }

    @Test
    @WithMockUser(roles = "content_manager")
    void updateLandingPage_Success() throws Exception {
        Long id = 1L;
        LandingPageDTO inputDto = LandingPageDTO.builder()
                .title("Updated Landing Page")
                .description("Updated Description")
                .metadata("{\"key\":\"updated\"}")
                .build();

        LandingPageDTO outputDto = LandingPageDTO.builder()
                .id(id)
                .title("Updated Landing Page")
                .description("Updated Description")
                .metadata("{\"key\":\"updated\"}")
                .build();

        when(landingPageService.updateLandingPage(eq(id), any(LandingPageDTO.class))).thenReturn(outputDto);

        mockMvc.perform(put("/api/v1/landing-pages/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Updated Landing Page"))
                .andExpect(jsonPath("$.description").value("Updated Description"))
                .andExpect(jsonPath("$.metadata").value("{\"key\":\"updated\"}"));
    }

    @Test
    void getAllLandingPages_Success() throws Exception {
        when(landingPageService.getAllLandingPages()).thenReturn(
                java.util.List.of(
                        LandingPageDTO.builder().id(1L).title("Page 1").description("Desc 1").build(),
                        LandingPageDTO.builder().id(2L).title("Page 2").description("Desc 2").build()
                )
        );

        mockMvc.perform(get("/api/v1/landing-pages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Page 1"))
                .andExpect(jsonPath("$[1].title").value("Page 2"));
    }

    @Test
    void getLandingPage_Success() throws Exception {
        Long id = 1L;
        LandingPageDTO landingPage = LandingPageDTO.builder()
                .id(id)
                .title("Test Page")
                .description("Test Description")
                .metadata("{\"key\":\"value\"}")
                .build();

        when(landingPageService.getLandingPage(id)).thenReturn(landingPage);

        mockMvc.perform(get("/api/v1/landing-pages/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Test Page"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.metadata").value("{\"key\":\"value\"}"));
    }
}
