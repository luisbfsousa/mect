package com.shophub.controller;

import com.shophub.dto.BannerDTO;
import com.shophub.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
public class BannerController {
    private final BannerService bannerService;

    @PostMapping
    @PreAuthorize("hasRole('content_manager')")
    public ResponseEntity<BannerDTO> createBanner(@RequestBody BannerDTO dto) {
        BannerDTO created = bannerService.createBanner(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('content_manager')")
    public ResponseEntity<BannerDTO> updateBanner(@PathVariable Long id, @RequestBody BannerDTO dto) {
        BannerDTO updated = bannerService.updateBanner(id, dto);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasRole('content_manager')")
    public ResponseEntity<BannerDTO> publishBanner(@PathVariable Long id) {
        BannerDTO published = bannerService.publishBanner(id);
        return ResponseEntity.ok(published);
    }

    @GetMapping
    @PreAuthorize("hasRole('content_manager')")
    public ResponseEntity<List<BannerDTO>> getAllBanners() {
        List<BannerDTO> banners = bannerService.getAllBanners();
        return ResponseEntity.ok(banners);
    }

    @GetMapping("/active")
    public ResponseEntity<List<BannerDTO>> getActiveBanners() {
        List<BannerDTO> banners = bannerService.getActiveBanners();
        return ResponseEntity.ok(banners);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BannerDTO> getBanner(@PathVariable Long id) {
        BannerDTO dto = bannerService.getBanner(id);
        return ResponseEntity.ok(dto);
    }
}
