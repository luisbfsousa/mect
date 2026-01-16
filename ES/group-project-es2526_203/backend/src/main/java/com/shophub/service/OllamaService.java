package com.shophub.service;

import com.shophub.dto.OllamaRequest;
import com.shophub.dto.OllamaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class OllamaService {

    private static final Logger logger = LoggerFactory.getLogger(OllamaService.class);

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:gemma3:1b}")
    private String defaultModel;

    private final RestTemplate restTemplate;

    public OllamaService() {
        this.restTemplate = new RestTemplate();
    }

    @Retryable(
        retryFor = {RestClientException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public OllamaResponse generate(String prompt) {
        return generate(prompt, defaultModel);
    }

    @Retryable(
        retryFor = {RestClientException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public OllamaResponse generate(String prompt, String model) {
        long startTime = System.currentTimeMillis();

        try {
            String url = ollamaBaseUrl + "/api/generate";

            OllamaRequest request = new OllamaRequest(model, prompt);
            request.setStream(false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<OllamaRequest> entity = new HttpEntity<>(request, headers);

            logger.debug("Sending request to Ollama: model={}, prompt_length={}",
                model, prompt.length());

            OllamaResponse response = restTemplate.postForObject(url, entity, OllamaResponse.class);

            long latency = System.currentTimeMillis() - startTime;
            logger.info("Ollama response received: latency={}ms, tokens={}",
                latency, response != null ? response.getTotalTokens() : 0);

            return response;

        } catch (Exception e) {
            logger.error("Error calling Ollama API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate response from LLM", e);
        }
    }

    public boolean isAvailable() {
        try {
            String url = ollamaBaseUrl + "/api/tags";
            restTemplate.getForObject(url, String.class);
            return true;
        } catch (Exception e) {
            logger.warn("Ollama service not available: {}", e.getMessage());
            return false;
        }
    }
}
