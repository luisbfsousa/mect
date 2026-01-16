package com.shophub.service;

import com.shophub.model.Product;
import com.shophub.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ProductContextService - Simplified RAG Implementation (Level 1-2)
 *
 * This service provides context to the LLM by retrieving relevant product information
 * from the database. For MLOps Level 1-2, we use simple keyword matching rather than
 * vector embeddings.
 */
@Service
public class ProductContextService {

    private static final Logger logger = LoggerFactory.getLogger(ProductContextService.class);
    private static final int MAX_PRODUCTS_IN_CONTEXT = 5;

    @Autowired
    private ProductRepository productRepository;

    /**
     * Build product context from user query using simple keyword matching
     */
    public String buildProductContext(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "No specific products to show. User can browse our catalog.";
        }

        // Extract potential product keywords (simple approach for Level 1-2)
        List<String> keywords = extractKeywords(query.toLowerCase());

        if (keywords.isEmpty()) {
            return "No specific products to show. User can browse our catalog.";
        }

        // Find products matching keywords
        List<Product> relevantProducts = findProductsByKeywords(keywords);

        if (relevantProducts.isEmpty()) {
            logger.debug("No products found for query: {}", query);
            return "No products found matching the query. Suggest browsing categories.";
        }

        // Format products as context
        String context = formatProductsAsContext(relevantProducts);
        logger.info("Generated context with {} products for query: {}", relevantProducts.size(), query);

        return context;
    }

    /**
     * Extract keywords from query (simple tokenization)
     */
    private List<String> extractKeywords(String query) {
        // Remove common words and split
        String[] commonWords = {"what", "is", "are", "the", "a", "an", "do", "you", "have",
            "can", "i", "get", "show", "me", "tell", "about", "find", "looking", "for"};

        return Arrays.stream(query.split("\\s+"))
            .filter(word -> word.length() > 2)
            .filter(word -> !Arrays.asList(commonWords).contains(word))
            .collect(Collectors.toList());
    }

    /**
     * Find products by keyword matching (simple SQL LIKE)
     */
    private List<Product> findProductsByKeywords(List<String> keywords) {
        List<Product> products = productRepository.findAll(PageRequest.of(0, 50)).getContent();

        return products.stream()
            .filter(product -> matchesKeywords(product, keywords))
            .limit(MAX_PRODUCTS_IN_CONTEXT)
            .collect(Collectors.toList());
    }

    /**
     * Check if product matches any keywords
     */
    private boolean matchesKeywords(Product product, List<String> keywords) {
        String searchText = (product.getName() + " " + product.getDescription()).toLowerCase();

        return keywords.stream()
            .anyMatch(keyword -> searchText.contains(keyword));
    }

    /**
     * Format products as context string for LLM
     */
    private String formatProductsAsContext(List<Product> products) {
        StringBuilder context = new StringBuilder();
        context.append("Here are relevant products from our catalog:\n\n");

        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            context.append(String.format("%d. %s\n", i + 1, p.getName()));
            context.append(String.format("   Price: $%.2f\n", p.getPrice()));

            if (p.getDescription() != null && !p.getDescription().isEmpty()) {
                String desc = p.getDescription();
                if (desc.length() > 150) {
                    desc = desc.substring(0, 150) + "...";
                }
                context.append(String.format("   Description: %s\n", desc));
            }

            context.append("\n");
        }

        return context.toString();
    }
}
