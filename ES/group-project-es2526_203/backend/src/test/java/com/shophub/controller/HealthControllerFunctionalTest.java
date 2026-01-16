package com.shophub.controller;

import com.shophub.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class HealthControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Transactional
    void healthCheck_returnsOkStatus() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("ok")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @Transactional
    void healthCheck_returnsTimestamp() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    @Transactional
    void healthCheck_publicEndpoint_noAuthRequired() throws Exception {
        // Act and Assert - should work without authentication
        mockMvc.perform(get("/api/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")));
    }

    @Test
    @Transactional
    void healthCheck_multipleConsecutiveCalls_succeed() throws Exception {
        // Act and Assert - multiple calls should all succeed
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/health")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("ok")))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Test
    @Transactional
    void healthCheck_withAcceptHeader_returnsJson() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("ok")));
    }

    @Test
    @Transactional
    void healthCheck_withoutContentType_stillWorks() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @Transactional
    void healthCheck_responseContainsOnlyExpectedFields() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.*", hasSize(2))); // Only 2 fields
    }

    @Test
    @Transactional
    void healthCheck_timestampIsValidFormat() throws Exception {
        // Act and Assert - timestamp should be in ISO format
        mockMvc.perform(get("/api/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp", matchesPattern(
                        "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")));
    }
}
