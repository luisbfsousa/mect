package com.shophub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shophub.dto.BannerDTO;
import com.shophub.exception.InvalidInputException;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.model.Banner;
import com.shophub.repository.BannerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BannerServiceTest {

    @Mock
    private BannerRepository bannerRepository;

    @InjectMocks
    private BannerService bannerService;

    private ObjectMapper objectMapper;
    private JsonNode testMetadata;
    private Banner testBanner;
    private BannerDTO testDTO;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        testMetadata = objectMapper.readTree("{\"cta\":\"Shop Now\"}");

        testBanner = Banner.builder()
                .id(1L)
                .title("Test Banner")
                .description("Test Description")
                .imageUrl("https://example.com/banner.jpg")
                .metadata(testMetadata)
                .startAt(LocalDateTime.now().minusDays(1))
                .endAt(LocalDateTime.now().plusDays(1))
                .isPublished(true)
                .priority(10)
                .landingPageId(1L)
                .build();

        testDTO = BannerDTO.builder()
                .title("Test Banner")
                .description("Test Description")
                .imageUrl("https://example.com/banner.jpg")
                .metadata("{\"cta\":\"Shop Now\"}")
                .startAt(LocalDateTime.now().minusDays(1).toString())
                .endAt(LocalDateTime.now().plusDays(1).toString())
                .isPublished(true)
                .priority(10)
                .landingPageId(1L)
                .build();
    }

    @Test
    void createBanner_ShouldCreateSuccessfully() {
        when(bannerRepository.save(any(Banner.class))).thenReturn(testBanner);

        BannerDTO result = bannerService.createBanner(testDTO);

        assertNotNull(result);
        assertEquals("Test Banner", result.getTitle());
        assertEquals("Test Description", result.getDescription());
        verify(bannerRepository).save(any(Banner.class));
    }

    @Test
    void createBanner_ShouldThrowExceptionWhenMetadataIsInvalidJson() {
        BannerDTO dtoWithInvalidMetadata = BannerDTO.builder()
                .title("Banner")
                .description("Description")
                .imageUrl("https://example.com/banner.jpg")
                .metadata("{invalid json}")
                .isPublished(false)
                .build();

        assertThrows(InvalidInputException.class,
                () -> bannerService.createBanner(dtoWithInvalidMetadata));
        verify(bannerRepository, never()).save(any(Banner.class));
    }

    @Test
    void updateBanner_ShouldUpdateSuccessfully() throws Exception {
        when(bannerRepository.findById(1L)).thenReturn(Optional.of(testBanner));

        BannerDTO updateDTO = BannerDTO.builder()
                .title("Updated Banner")
                .description("Updated Description")
                .imageUrl("https://example.com/updated.jpg")
                .metadata("{\"cta\":\"Buy Now\"}")
                .isPublished(false)
                .priority(20)
                .build();

        Banner updatedBanner = Banner.builder()
                .id(1L)
                .title("Updated Banner")
                .description("Updated Description")
                .imageUrl("https://example.com/updated.jpg")
                .metadata(objectMapper.readTree("{\"cta\":\"Buy Now\"}"))
                .isPublished(false)
                .priority(20)
                .build();

        when(bannerRepository.save(any(Banner.class))).thenReturn(updatedBanner);

        BannerDTO result = bannerService.updateBanner(1L, updateDTO);

        assertNotNull(result);
        assertEquals("Updated Banner", result.getTitle());
        assertFalse(result.isPublished());
        verify(bannerRepository).findById(1L);
        verify(bannerRepository).save(any(Banner.class));
    }

    @Test
    void updateBanner_ShouldThrowExceptionWhenBannerNotFound() {
        when(bannerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bannerService.updateBanner(999L, testDTO));
        verify(bannerRepository).findById(999L);
        verify(bannerRepository, never()).save(any());
    }

    @Test
    void publishBanner_ShouldPublishSuccessfully() {
        Banner unpublishedBanner = Banner.builder()
                .id(1L)
                .title("Unpublished Banner")
                .isPublished(false)
                .build();

        Banner publishedBanner = Banner.builder()
                .id(1L)
                .title("Unpublished Banner")
                .isPublished(true)
                .build();

        when(bannerRepository.findById(1L)).thenReturn(Optional.of(unpublishedBanner));
        when(bannerRepository.save(any(Banner.class))).thenReturn(publishedBanner);

        BannerDTO result = bannerService.publishBanner(1L);

        assertNotNull(result);
        assertTrue(result.isPublished());
        verify(bannerRepository).findById(1L);
        verify(bannerRepository).save(any(Banner.class));
    }

    @Test
    void publishBanner_ShouldThrowExceptionWhenBannerNotFound() {
        when(bannerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bannerService.publishBanner(999L));
        verify(bannerRepository).findById(999L);
        verify(bannerRepository, never()).save(any());
    }

    @Test
    void getAllBanners_ShouldReturnAllBanners() {
        Banner banner2 = Banner.builder()
                .id(2L)
                .title("Second Banner")
                .build();

        when(bannerRepository.findAll()).thenReturn(Arrays.asList(testBanner, banner2));

        List<BannerDTO> result = bannerService.getAllBanners();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(bannerRepository).findAll();
    }

    @Test
    void getBanner_ShouldReturnBanner() {
        when(bannerRepository.findById(1L)).thenReturn(Optional.of(testBanner));

        BannerDTO result = bannerService.getBanner(1L);

        assertNotNull(result);
        assertEquals("Test Banner", result.getTitle());
        verify(bannerRepository).findById(1L);
    }

    @Test
    void getBanner_ShouldThrowExceptionWhenBannerNotFound() {
        when(bannerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bannerService.getBanner(999L));
        verify(bannerRepository).findById(999L);
    }

    @Test
    void getActiveBanners_ShouldReturnOnlyCurrentlyRunningBanners() {
        LocalDateTime now = LocalDateTime.now();

        Banner activeBanner = Banner.builder()
                .id(1L)
                .title("Active Banner")
                .isPublished(true)
                .startAt(now.minusDays(1))
                .endAt(now.plusDays(1))
                .priority(10)
                .build();

        Banner futureBanner = Banner.builder()
                .id(2L)
                .title("Future Banner")
                .isPublished(true)
                .startAt(now.plusDays(1))
                .endAt(now.plusDays(5))
                .priority(5)
                .build();

        Banner expiredBanner = Banner.builder()
                .id(3L)
                .title("Expired Banner")
                .isPublished(true)
                .startAt(now.minusDays(5))
                .endAt(now.minusDays(1))
                .priority(15)
                .build();

        Banner unpublishedBanner = Banner.builder()
                .id(4L)
                .title("Unpublished Banner")
                .isPublished(false)
                .startAt(now.minusDays(1))
                .endAt(now.plusDays(1))
                .priority(20)
                .build();

        when(bannerRepository.findAll()).thenReturn(Arrays.asList(
                activeBanner, futureBanner, expiredBanner, unpublishedBanner
        ));

        List<BannerDTO> result = bannerService.getActiveBanners();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Active Banner", result.get(0).getTitle());
        verify(bannerRepository).findAll();
    }

    @Test
    void getActiveBanners_ShouldSortByPriorityDescending() {
        Banner lowPriority = Banner.builder()
                .id(1L)
                .title("Low Priority")
                .isPublished(true)
                .priority(5)
                .build();

        Banner highPriority = Banner.builder()
                .id(2L)
                .title("High Priority")
                .isPublished(true)
                .priority(20)
                .build();

        Banner mediumPriority = Banner.builder()
                .id(3L)
                .title("Medium Priority")
                .isPublished(true)
                .priority(10)
                .build();

        when(bannerRepository.findAll()).thenReturn(Arrays.asList(lowPriority, highPriority, mediumPriority));

        List<BannerDTO> result = bannerService.getActiveBanners();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("High Priority", result.get(0).getTitle());
        assertEquals("Medium Priority", result.get(1).getTitle());
        assertEquals("Low Priority", result.get(2).getTitle());
    }

    @Test
    void getActiveBanners_ShouldFilterByPublishedStatus() {
        Banner published = Banner.builder()
                .id(1L)
                .title("Published")
                .isPublished(true)
                .build();

        Banner unpublished = Banner.builder()
                .id(2L)
                .title("Unpublished")
                .isPublished(false)
                .build();

        when(bannerRepository.findAll()).thenReturn(Arrays.asList(published, unpublished));

        List<BannerDTO> result = bannerService.getActiveBanners();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Published", result.get(0).getTitle());
    }
}
