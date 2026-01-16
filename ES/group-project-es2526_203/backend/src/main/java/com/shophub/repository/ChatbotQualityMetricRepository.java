package com.shophub.repository;

import com.shophub.model.ChatbotQualityMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatbotQualityMetricRepository extends JpaRepository<ChatbotQualityMetric, UUID> {

    Optional<ChatbotQualityMetric> findByMessageId(UUID messageId);
}
