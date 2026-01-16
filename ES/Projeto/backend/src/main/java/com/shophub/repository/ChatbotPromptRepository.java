package com.shophub.repository;

import com.shophub.model.ChatbotPrompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatbotPromptRepository extends JpaRepository<ChatbotPrompt, UUID> {

    Optional<ChatbotPrompt> findByActiveTrue();

    Optional<ChatbotPrompt> findByVersion(String version);
}
