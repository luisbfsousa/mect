package com.shophub.controller;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private OpenTelemetry openTelemetry;

    @Mock
    private Tracer tracer;

    private HealthController controller;

    @BeforeEach
    void setUp() {
        when(openTelemetry.getTracer(anyString())).thenReturn(tracer);
        controller = new HealthController(openTelemetry);
    }

    @Test
    void healthCheck_ShouldReturnOkStatus() {
        ResponseEntity<Map<String, Object>> response = controller.healthCheck();
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void healthCheck_ShouldContainStatusField() {
        ResponseEntity<Map<String, Object>> response = controller.healthCheck();
        assertEquals("ok", response.getBody().get("status"));
    }

    @Test
    void healthCheck_ShouldContainTimestamp() {
        ResponseEntity<Map<String, Object>> response = controller.healthCheck();
        assertTrue(response.getBody().get("timestamp") instanceof LocalDateTime);
    }
}
