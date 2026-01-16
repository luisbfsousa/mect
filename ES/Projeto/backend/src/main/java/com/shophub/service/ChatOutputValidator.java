package com.shophub.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * ChatOutputValidator - Quality gates for LLM outputs (Level 1-2)
 *
 * Validates chatbot responses before sending to users
 */
@Component
public class ChatOutputValidator {

    private static final Logger logger = LoggerFactory.getLogger(ChatOutputValidator.class);

    private static final int MIN_LENGTH = 10;
    private static final int MAX_LENGTH = 1000;

    // Basic inappropriate content detection (keyword-based for Level 1-2)
    private static final List<String> INAPPROPRIATE_KEYWORDS = Arrays.asList(
        "offensive_placeholder1", // Replace with actual list in production
        "offensive_placeholder2"
    );

    public ValidationResult validate(String output) {
        ValidationResult result = new ValidationResult();
        result.setValid(true);

        if (output == null || output.trim().isEmpty()) {
            result.setValid(false);
            result.setReason("Output is empty");
            logger.warn("Validation failed: empty output");
            return result;
        }

        // Check length
        if (output.length() < MIN_LENGTH) {
            result.setValid(false);
            result.setReason("Output too short");
            logger.warn("Validation failed: output too short ({})", output.length());
            return result;
        }

        if (output.length() > MAX_LENGTH) {
            result.setValid(false);
            result.setReason("Output too long");
            logger.warn("Validation failed: output too long ({})", output.length());
            return result;
        }

        // Check for inappropriate content (basic keyword matching)
        String lowerOutput = output.toLowerCase();
        for (String keyword : INAPPROPRIATE_KEYWORDS) {
            if (lowerOutput.contains(keyword)) {
                result.setValid(false);
                result.setReason("Inappropriate content detected");
                logger.warn("Validation failed: inappropriate content");
                return result;
            }
        }

        logger.debug("Output validation passed");
        return result;
    }

    public static class ValidationResult {
        private boolean valid;
        private String reason;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
