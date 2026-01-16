package com.shophub.repository;

import com.shophub.model.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {
    List<Banner> findByIsPublishedTrueAndStartAtBeforeAndEndAtAfter(LocalDateTime before, LocalDateTime after);
}
