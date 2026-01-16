package com.shophub.service;

import com.shophub.dto.ChatRequest;
import com.shophub.dto.ChatResponse;
import com.shophub.dto.OllamaResponse;
import com.shophub.model.ChatbotConversation;
import com.shophub.model.ChatbotMessage;
import com.shophub.repository.ChatbotConversationRepository;
import com.shophub.repository.ChatbotMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotService.class);

    @Autowired
    private OllamaService ollamaService;

    @Autowired
    private PromptService promptService;

    @Autowired
    private ProductContextService productContextService;

    @Autowired
    private ChatOutputValidator outputValidator;

    @Autowired
    private ChatbotConversationRepository conversationRepository;

    @Autowired
    private ChatbotMessageRepository messageRepository;

    @Transactional
    public ChatResponse chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. Get or create conversation
            ChatbotConversation conversation = getOrCreateConversation(request.getSessionId(), request.getUserId());

            // 2. Save user message
            ChatbotMessage userMessage = new ChatbotMessage("user", request.getQuery());
            userMessage.setConversation(conversation);
            messageRepository.save(userMessage);
            conversation.incrementMessageCount();

            // 3. Build RAG context
            String context = productContextService.buildProductContext(request.getQuery());

            // 4. Get conversation history (last 5 messages)
            String history = buildConversationHistory(conversation);

            // 5. Render prompt template
            String prompt = promptService.renderPrompt(context, history, request.getQuery());

            // 6. Call Ollama
            OllamaResponse ollamaResponse = ollamaService.generate(prompt);

            if (ollamaResponse == null || ollamaResponse.getResponse() == null) {
                throw new RuntimeException("No response from LLM");
            }

            String responseText = ollamaResponse.getResponse();

            // 7. Validate output
            ChatOutputValidator.ValidationResult validation = outputValidator.validate(responseText);
            if (!validation.isValid()) {
                logger.warn("Output validation failed: {}", validation.getReason());
                responseText = "I apologize, but I need to rephrase that. Could you please ask your question again?";
            }

            // 8. Calculate metrics
            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            int tokensUsed = ollamaResponse.getTotalTokens();

            // 9. Save assistant message
            ChatbotMessage assistantMessage = new ChatbotMessage("assistant", responseText);
            assistantMessage.setConversation(conversation);
            assistantMessage.setTokensUsed(tokensUsed);
            assistantMessage.setLatencyMs(latencyMs);
            assistantMessage = messageRepository.save(assistantMessage);
            conversation.incrementMessageCount();

            conversationRepository.save(conversation);

            // 10. Build response
            ChatResponse response = new ChatResponse(responseText, request.getSessionId());
            response.setMessageId(assistantMessage.getId().toString());
            response.setLatencyMs(latencyMs);
            response.setTokensUsed(tokensUsed);

            logger.info("Chat response generated: session={}, latency={}ms, tokens={}",
                request.getSessionId(), latencyMs, tokensUsed);

            return response;

        } catch (Exception e) {
            logger.error("Error processing chat request: {}", e.getMessage(), e);

            // Return fallback response
            ChatResponse fallbackResponse = new ChatResponse(
                "I apologize, but I'm having trouble processing your request. Please try again or contact support.",
                request.getSessionId()
            );
            fallbackResponse.setLatencyMs((int) (System.currentTimeMillis() - startTime));

            return fallbackResponse;
        }
    }

    private ChatbotConversation getOrCreateConversation(String sessionId, String userId) {
        return conversationRepository.findBySessionIdAndEndedAtIsNull(sessionId)
            .orElseGet(() -> {
                ChatbotConversation newConversation = new ChatbotConversation(sessionId, userId);
                return conversationRepository.save(newConversation);
            });
    }

    private String buildConversationHistory(ChatbotConversation conversation) {
        List<ChatbotMessage> recentMessages = messageRepository
            .findByConversationIdOrderByCreatedAtAsc(conversation.getId())
            .stream()
            .skip(Math.max(0, conversation.getTotalMessages() - 5))
            .collect(Collectors.toList());

        if (recentMessages.isEmpty()) {
            return "No previous conversation.";
        }

        StringBuilder history = new StringBuilder();
        for (ChatbotMessage msg : recentMessages) {
            history.append(msg.getRole().toUpperCase())
                .append(": ")
                .append(msg.getContent())
                .append("\n");
        }

        return history.toString();
    }

    public List<ChatbotMessage> getConversationHistory(String sessionId) {
        return conversationRepository.findBySessionId(sessionId)
            .map(conversation -> messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId()))
            .orElse(List.of());
    }

    @Transactional
    public void endConversation(String sessionId) {
        conversationRepository.findBySessionId(sessionId).ifPresent(conversation -> {
            conversation.setEndedAt(LocalDateTime.now());
            conversationRepository.save(conversation);
            logger.info("Ended conversation: session={}", sessionId);
        });
    }
}
