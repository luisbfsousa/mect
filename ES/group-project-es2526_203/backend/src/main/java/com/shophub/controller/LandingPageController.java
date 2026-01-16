package com.shophub.controller;

import com.shophub.dto.LandingPageDTO;
import com.shophub.service.LandingPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/landing-pages")
@RequiredArgsConstructor
public class LandingPageController {
    private final LandingPageService landingPageService;

    @PostMapping
    @PreAuthorize("hasRole('content_manager')")
    public ResponseEntity<LandingPageDTO> createLandingPage(@RequestBody LandingPageDTO landingPageDTO) {
        LandingPageDTO createdPage = landingPageService.createLandingPage(landingPageDTO);
        return new ResponseEntity<>(createdPage, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('content_manager')")
    public ResponseEntity<LandingPageDTO> updateLandingPage(@PathVariable Long id, @RequestBody LandingPageDTO landingPageDTO) {
        LandingPageDTO updatedPage = landingPageService.updateLandingPage(id, landingPageDTO);
        return ResponseEntity.ok(updatedPage);
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasRole('content_manager')")
    public ResponseEntity<LandingPageDTO> publishLandingPage(@PathVariable Long id) {
        LandingPageDTO publishedPage = landingPageService.publishLandingPage(id);
        return ResponseEntity.ok(publishedPage);
    }

    @PutMapping("/{id}/unpublish")
    @PreAuthorize("hasRole('content_manager')")
    public ResponseEntity<LandingPageDTO> unpublishLandingPage(@PathVariable Long id) {
        LandingPageDTO updatedPage = landingPageService.unpublishLandingPage(id);
        return ResponseEntity.ok(updatedPage);
    }

    @GetMapping
    public ResponseEntity<List<LandingPageDTO>> getAllLandingPages() {
        List<LandingPageDTO> pages = landingPageService.getAllLandingPages();
        return ResponseEntity.ok(pages);
    }

    @GetMapping("/published")
    public ResponseEntity<List<LandingPageDTO>> getPublishedLandingPages() {
        List<LandingPageDTO> pages = landingPageService.getPublishedLandingPages();
        return ResponseEntity.ok(pages);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LandingPageDTO> getLandingPage(@PathVariable Long id) {
        LandingPageDTO page = landingPageService.getLandingPage(id);
        return ResponseEntity.ok(page);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('content_manager')")
    public ResponseEntity<Void> deleteLandingPage(@PathVariable Long id) {
        landingPageService.deleteLandingPage(id);
        return ResponseEntity.noContent().build();
    }
}