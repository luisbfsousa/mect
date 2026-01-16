package com.shophub.repository;

import com.shophub.model.ChatbotConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatbotConversationRepository extends JpaRepository<ChatbotConversation, UUID> {

    Optional<ChatbotConversation> findBySessionId(String sessionId);

    Optional<ChatbotConversation> findBySessionIdAndEndedAtIsNull(String sessionId);
}
