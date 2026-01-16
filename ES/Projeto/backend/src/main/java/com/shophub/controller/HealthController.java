package com.shophub.controller;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final Tracer tracer;

    public HealthController(OpenTelemetry openTelemetry) {
        this.tracer = openTelemetry.getTracer("application");
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "timestamp", LocalDateTime.now()
        ));
    }
}
