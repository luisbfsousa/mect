package com.shophub.repository;

import com.shophub.model.LandingPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LandingPageRepository extends JpaRepository<LandingPage, Long> {
    List<LandingPage> findByIsPublishedTrue();
}