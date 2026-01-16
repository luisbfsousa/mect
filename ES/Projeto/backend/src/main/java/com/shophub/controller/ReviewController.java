package com.shophub.controller;

import com.shophub.model.Review;
import com.shophub.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    
    private final ReviewService reviewService;
    
    // Get all reviews for a specific product
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Review>> getReviewsByProduct(@PathVariable Integer productId) {
        List<Review> reviews = reviewService.getReviewsByProductId(productId);
        return ResponseEntity.ok(reviews);
    }
    
    // Get review statistics for a product
    @GetMapping("/product/{productId}/stats")
    public ResponseEntity<Map<String, Object>> getProductReviewStats(@PathVariable Integer productId) {
        Map<String, Object> stats = reviewService.getProductReviewStats(productId);
        return ResponseEntity.ok(stats);
    }
    
    // Get reviews by user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Review>> getReviewsByUser(@PathVariable String userId) {
        List<Review> reviews = reviewService.getReviewsByUserId(userId);
        return ResponseEntity.ok(reviews);
    }
    
    // Create a new review
    @PostMapping
    public ResponseEntity<Review> createReview(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Review review) {
        String userId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        
        // Extract roles from JWT
        List<String> roles = extractRolesFromJwt(jwt);
        
        Review createdReview = reviewService.createReview(review, userId, username, roles, jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdReview);
    }
    
    // Update a review
    @PutMapping("/{reviewId}")
    public ResponseEntity<Review> updateReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer reviewId,
            @RequestBody Review review) {
        String userId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        
        // Extract roles from JWT
        List<String> roles = extractRolesFromJwt(jwt);
        
        Review updatedReview = reviewService.updateReview(reviewId, review, userId, username, roles);
        return ResponseEntity.ok(updatedReview);
    }
    
    // Delete a review
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer reviewId) {
        String userId = jwt.getSubject();
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.noContent().build();
    }
    
    // Helper method to extract roles from JWT
    private List<String> extractRolesFromJwt(Jwt jwt) {
        List<String> roles = new ArrayList<>();
        
        // Extract realm_access roles
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") != null) {
            @SuppressWarnings("unchecked")
            List<String> realmRoles = (List<String>) realmAccess.get("roles");
            roles.addAll(realmRoles);
        }
        
        // Also extract resource_access roles (client-specific roles)
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            resourceAccess.forEach((client, clientRoles) -> {
                if (clientRoles instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> clientMap = (Map<String, Object>) clientRoles;
                    Object rolesObj = clientMap.get("roles");
                    if (rolesObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> clientRolesList = (List<String>) rolesObj;
                        roles.addAll(clientRolesList);
                    }
                }
            });
        }
        
        return roles;
    }
}
