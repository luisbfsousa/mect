package com.shophub.service;

import com.shophub.model.ChatbotPrompt;
import com.shophub.repository.ChatbotPromptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PromptService {

    private static final Logger logger = LoggerFactory.getLogger(PromptService.class);

    @Autowired
    private ChatbotPromptRepository promptRepository;

    public String getActivePromptTemplate() {
        return promptRepository.findByActiveTrue()
            .map(ChatbotPrompt::getTemplate)
            .orElseThrow(() -> new RuntimeException("No active prompt template found"));
    }

    public String renderPrompt(String template, Map<String, String> variables) {
        String rendered = template;

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            rendered = rendered.replace(placeholder, value);
        }

        logger.debug("Rendered prompt: length={}", rendered.length());
        return rendered;
    }

    public String renderPrompt(String context, String history, String query) {
        String template = getActivePromptTemplate();

        Map<String, String> variables = new HashMap<>();
        variables.put("context", context);
        variables.put("history", history);
        variables.put("query", query);

        return renderPrompt(template, variables);
    }
}
