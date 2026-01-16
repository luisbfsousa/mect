package com.shophub.controller;

import com.shophub.dto.BannerDTO;
import com.shophub.service.BannerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BannerControllerTest {

    @Mock
    private BannerService bannerService;

    @InjectMocks
    private BannerController bannerController;

    private BannerDTO sample;

    @BeforeEach
    void setUp() {
        sample = BannerDTO.builder()
                .id(1L)
                .title("Spring Sale")
                .description("Up to 50% off!")
                .imageUrl("https://example.com/spring.jpg")
                .metadata("{\"key\":\"value\"}")
                .startAt("2025-01-01T00:00:00")
                .endAt("2025-01-31T23:59:59")
                .isPublished(true)
                .priority(10)
                .build();
    }

    @Test
    void createBanner_returnsCreatedBannerAndStatus201() {
        when(bannerService.createBanner(any(BannerDTO.class))).thenReturn(sample);

        BannerDTO input = BannerDTO.builder()
                .title("Spring Sale")
                .description("Up to 50% off!")
                .imageUrl("https://example.com/spring.jpg")
                .build();

        ResponseEntity<BannerDTO> response = bannerController.createBanner(input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
        verify(bannerService, times(1)).createBanner(any(BannerDTO.class));
    }

    @Test
    void updateBanner_returnsUpdatedBanner() {
        when(bannerService.updateBanner(eq(1L), any(BannerDTO.class))).thenReturn(sample);

        BannerDTO update = BannerDTO.builder().title("Spring Sale").build();
        ResponseEntity<BannerDTO> response = bannerController.updateBanner(1L, update);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Spring Sale");
        verify(bannerService).updateBanner(eq(1L), any(BannerDTO.class));
    }

    @Test
    void publishBanner_returnsPublishedBanner() {
        BannerDTO published = BannerDTO.builder().id(1L).title("Spring Sale").isPublished(true).build();
        when(bannerService.publishBanner(1L)).thenReturn(published);

        ResponseEntity<BannerDTO> response = bannerController.publishBanner(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isPublished()).isTrue();
        verify(bannerService).publishBanner(1L);
    }

    @Test
    void getAllBanners_returnsList() {
        when(bannerService.getAllBanners()).thenReturn(Arrays.asList(sample));

        ResponseEntity<List<BannerDTO>> response = bannerController.getAllBanners();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        verify(bannerService).getAllBanners();
    }

    @Test
    void getActiveBanners_returnsActiveList() {
        when(bannerService.getActiveBanners()).thenReturn(List.of(sample));

        ResponseEntity<List<BannerDTO>> response = bannerController.getActiveBanners();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        verify(bannerService).getActiveBanners();
    }

    @Test
    void getBanner_returnsBannerById() {
        when(bannerService.getBanner(1L)).thenReturn(sample);

        ResponseEntity<BannerDTO> response = bannerController.getBanner(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
        verify(bannerService).getBanner(1L);
    }
}
