package com.shophub.service;

import com.shophub.dto.BannerDTO;
import com.shophub.model.Banner;
import com.shophub.repository.BannerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class BannerServiceIntegrationTest {

    @Autowired
    private BannerService bannerService;

    @Autowired
    private BannerRepository bannerRepository;

    @AfterEach
    void cleanup() {
        bannerRepository.deleteAll();
    }

    private Banner createBanner(String title, boolean published, LocalDateTime start, LocalDateTime end, Integer priority) {
        return Banner.builder()
                .title(title)
                .description("desc")
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
    void getActiveBanners_returnsOnlyCurrentlyRunningBanners() {
        LocalDateTime now = LocalDateTime.now();
        bannerRepository.save(createBanner("Active", true, now.minusDays(1), now.plusDays(1), 10));
        bannerRepository.save(createBanner("Future", true, now.plusDays(1), now.plusDays(2), 5));
        bannerRepository.save(createBanner("Expired", true, now.minusDays(5), now.minusDays(1), 15));
        bannerRepository.save(createBanner("Unpublished", false, now.minusDays(1), now.plusDays(1), 20));

        List<BannerDTO> result = bannerService.getActiveBanners();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Active");
    }

    @Test
    @Transactional
    void getActiveBanners_includesBannerWithNoDates() {
        bannerRepository.save(createBanner("Always Active", true, null, null, 5));
        List<BannerDTO> result = bannerService.getActiveBanners();
        assertThat(result).extracting("title").contains("Always Active");
    }

    @Test
    @Transactional
    void getActiveBanners_includesBannerWithOnlyStartDate() {
        LocalDateTime now = LocalDateTime.now();
        bannerRepository.save(createBanner("Started", true, now.minusDays(1), null, 10));
        bannerRepository.save(createBanner("Future", true, now.plusDays(1), null, 5));
        List<BannerDTO> result = bannerService.getActiveBanners();
        assertThat(result).extracting("title").contains("Started");
        assertThat(result).extracting("title").doesNotContain("Future");
    }

    @Test
    @Transactional
    void getActiveBanners_includesBannerWithOnlyEndDate() {
        LocalDateTime now = LocalDateTime.now();
        bannerRepository.save(createBanner("Ending", true, null, now.plusDays(1), 10));
        bannerRepository.save(createBanner("Ended", true, null, now.minusDays(1), 5));
        List<BannerDTO> result = bannerService.getActiveBanners();
        assertThat(result).extracting("title").contains("Ending");
        assertThat(result).extracting("title").doesNotContain("Ended");
    }

    @Test
    @Transactional
    void getActiveBanners_excludesUnpublishedBanners() {
        LocalDateTime now = LocalDateTime.now();
        bannerRepository.save(createBanner("Unpublished", false, now.minusDays(1), now.plusDays(1), 10));
        List<BannerDTO> result = bannerService.getActiveBanners();
        assertThat(result).isEmpty();
    }

    @Test
    @Transactional
    void getActiveBanners_handlesEdgeCases() {
        LocalDateTime now = LocalDateTime.now();
        bannerRepository.save(createBanner("Starting Now", true, now, now.plusDays(1), 10));
        bannerRepository.save(createBanner("Ending Now", true, now.minusDays(1), now, 5));
        List<BannerDTO> result = bannerService.getActiveBanners();
        assertThat(result).extracting("title").contains("Starting Now");
        assertThat(result).extracting("title").doesNotContain("Ending Now");
    }

    @Test
    @Transactional
    void getActiveBanners_returnsEmptyWhenAllExpiredOrFuture() {
        LocalDateTime now = LocalDateTime.now();
        bannerRepository.save(createBanner("Expired", true, now.minusDays(5), now.minusDays(1), 10));
        bannerRepository.save(createBanner("Future", true, now.plusDays(1), now.plusDays(2), 5));
        List<BannerDTO> result = bannerService.getActiveBanners();
        assertThat(result).isEmpty();
    }

    @Test
    @Transactional
    void getActiveBanners_handlesMixedScenarios() {
        LocalDateTime now = LocalDateTime.now();
        bannerRepository.save(createBanner("Active", true, now.minusDays(1), now.plusDays(1), 10));
        bannerRepository.save(createBanner("Always", true, null, null, 5));
        bannerRepository.save(createBanner("Started", true, now.minusDays(1), null, 8));
        bannerRepository.save(createBanner("Ending", true, null, now.plusDays(1), 7));
        bannerRepository.save(createBanner("Future", true, now.plusDays(1), now.plusDays(2), 6));
        bannerRepository.save(createBanner("Expired", true, now.minusDays(2), now.minusDays(1), 9));
        List<BannerDTO> result = bannerService.getActiveBanners();
        assertThat(result).extracting("title")
            .containsExactlyInAnyOrder("Active", "Always", "Started", "Ending");
    }

    @Test
    @Transactional
    void getActiveBanners_sortsByPriorityDescending() {
        LocalDateTime now = LocalDateTime.now();
        bannerRepository.save(createBanner("Low", true, now.minusDays(1), now.plusDays(1), 5));
        bannerRepository.save(createBanner("High", true, now.minusDays(1), now.plusDays(1), 20));
        bannerRepository.save(createBanner("Medium", true, now.minusDays(1), now.plusDays(1), 10));
        List<BannerDTO> result = bannerService.getActiveBanners();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getTitle()).isEqualTo("High");
        assertThat(result.get(1).getTitle()).isEqualTo("Medium");
        assertThat(result.get(2).getTitle()).isEqualTo("Low");
    }
}
