package com.shophub.controller;

import com.shophub.dto.ChatRequest;
import com.shophub.dto.ChatResponse;
import com.shophub.dto.FeedbackDTO;
import com.shophub.model.ChatbotMessage;
import com.shophub.model.ChatbotQualityMetric;
import com.shophub.repository.ChatbotMessageRepository;
import com.shophub.repository.ChatbotQualityMetricRepository;
import com.shophub.service.ChatbotService;
import com.shophub.service.OllamaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotController.class);

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private OllamaService ollamaService;

    @Autowired
    private ChatbotMessageRepository messageRepository;

    @Autowired
    private ChatbotQualityMetricRepository qualityMetricRepository;

    /**
     * POST /api/chatbot/chat
     * Main chat endpoint
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        logger.info("Chat request received: session={}, query_length={}",
            request.getSessionId(), request.getQuery() != null ? request.getQuery().length() : 0);

        // Validate request
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (request.getSessionId() == null || request.getSessionId().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Process chat
        ChatResponse response = chatbotService.chat(request);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/chatbot/conversations/{sessionId}
     * Get conversation history
     */
    @GetMapping("/conversations/{sessionId}")
    public ResponseEntity<List<ChatbotMessage>> getConversationHistory(@PathVariable String sessionId) {
        logger.debug("Getting conversation history: session={}", sessionId);

        List<ChatbotMessage> messages = chatbotService.getConversationHistory(sessionId);

        return ResponseEntity.ok(messages);
    }

    /**
     * POST /api/chatbot/conversations/{sessionId}/end
     * End conversation
     */
    @PostMapping("/conversations/{sessionId}/end")
    public ResponseEntity<Void> endConversation(@PathVariable String sessionId) {
        logger.info("Ending conversation: session={}", sessionId);

        chatbotService.endConversation(sessionId);

        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/chatbot/feedback
     * Submit user feedback
     */
    @PostMapping("/feedback")
    public ResponseEntity<Void> submitFeedback(@RequestBody FeedbackRequest request) {
        logger.info("Feedback received: message_id={}, feedback={}",
            request.getMessageId(), request.getFeedback());

        try {
            // Parse message ID
            UUID messageId = UUID.fromString(request.getMessageId());

            // Find the message
            ChatbotMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));

            // Check if feedback already exists
            ChatbotQualityMetric metric = qualityMetricRepository.findByMessageId(messageId)
                .orElse(new ChatbotQualityMetric(message, request.getFeedback()));

            // Update feedback
            metric.setUserFeedback(request.getFeedback());

            // Save to database
            qualityMetricRepository.save(metric);

            logger.info("Feedback stored successfully: message_id={}, feedback={}",
                messageId, request.getFeedback());

        } catch (IllegalArgumentException e) {
            logger.error("Invalid message ID format: {}", request.getMessageId());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error storing feedback: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/chatbot/health
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("ollama_available", ollamaService.isAvailable());

        return ResponseEntity.ok(health);
    }

    /**
     * GET /api/chatbot/feedback
     * Get all feedback metrics
     */
    @GetMapping("/feedback")
    public ResponseEntity<List<FeedbackDTO>> getAllFeedback() {
        logger.debug("Retrieving all feedback metrics");
        List<FeedbackDTO> feedbackList = qualityMetricRepository.findAll().stream()
            .map(FeedbackDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(feedbackList);
    }

    /**
     * GET /api/chatbot/feedback/stats
     * Get feedback statistics
     */
    @GetMapping("/feedback/stats")
    public ResponseEntity<Map<String, Object>> getFeedbackStats() {
        logger.debug("Calculating feedback statistics");

        List<ChatbotQualityMetric> allMetrics = qualityMetricRepository.findAll();

        long totalFeedback = allMetrics.stream()
            .filter(m -> m.getUserFeedback() != null)
            .count();

        long helpfulCount = allMetrics.stream()
            .filter(m -> "helpful".equals(m.getUserFeedback()))
            .count();

        long notHelpfulCount = allMetrics.stream()
            .filter(m -> "not_helpful".equals(m.getUserFeedback()))
            .count();

        long inappropriateCount = allMetrics.stream()
            .filter(m -> "inappropriate".equals(m.getUserFeedback()))
            .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total_feedback", totalFeedback);
        stats.put("helpful", helpfulCount);
        stats.put("not_helpful", notHelpfulCount);
        stats.put("inappropriate", inappropriateCount);

        if (totalFeedback > 0) {
            stats.put("helpful_percentage", (helpfulCount * 100.0) / totalFeedback);
            stats.put("not_helpful_percentage", (notHelpfulCount * 100.0) / totalFeedback);
            stats.put("inappropriate_percentage", (inappropriateCount * 100.0) / totalFeedback);
        }

        return ResponseEntity.ok(stats);
    }

    // DTOs
    public static class FeedbackRequest {
        private String messageId;
        private String feedback; // 'helpful', 'not_helpful', 'inappropriate'

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        public String getFeedback() {
            return feedback;
        }

        public void setFeedback(String feedback) {
            this.feedback = feedback;
        }
    }
}
