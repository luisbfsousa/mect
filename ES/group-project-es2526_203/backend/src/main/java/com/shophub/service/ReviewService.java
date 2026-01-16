package com.shophub.service;

import com.shophub.exception.ResourceNotFoundException;
import com.shophub.model.Review;
import com.shophub.model.User;
import com.shophub.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final UserService userService;
    
    @Transactional(readOnly = true)
    public List<Review> getReviewsByProductId(Integer productId) {
        log.info("Fetching reviews for product: {}", productId);
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }
    
    @Transactional(readOnly = true)
    public List<Review> getReviewsByUserId(String userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> getProductReviewStats(Integer productId) {
        Double avgRating = reviewRepository.getAverageRatingByProductId(productId);
        Long reviewCount = reviewRepository.getReviewCountByProductId(productId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("averageRating", avgRating != null ? avgRating : 0.0);
        stats.put("reviewCount", reviewCount != null ? reviewCount : 0L);
        
        return stats;
    }
    
    @Transactional
    public Review createReview(Review review, String userId, String username, List<String> roles, Jwt jwt) {
        // Validate user is a customer
        if (roles == null || !roles.contains("customer")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only customers can write reviews");
        }
        
        // Validate username
        if (username == null || !username.equals(review.getUserName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username in review must match authenticated user's username");
        }
        
        // Ensure user exists in database (create if doesn't exist)
        User user = userService.getOrCreateUser(jwt);

        // Block locked or deactivated accounts from writing reviews
        if (Boolean.TRUE.equals(user.getIsLocked())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account is locked and cannot write reviews.");
        }
        if (Boolean.TRUE.equals(user.getIsDeactivated())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account is deactivated and cannot write reviews.");
        }
        
        // Set the user ID from JWT
        review.setUserId(userId);
        
        // Check if user already reviewed this product
        if (reviewRepository.existsByUserIdAndProductId(userId, review.getProductId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User has already reviewed this product");
        }
        
        log.info("Creating review for product {} by user {}", review.getProductId(), username);
        return reviewRepository.save(review);
    }
    
    @Transactional
    public Review updateReview(Integer reviewId, Review updatedReview, String userId, String username, List<String> roles) {
        // Validate user is a customer
        if (roles == null || !roles.contains("customer")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only customers can update reviews");
        }
        
        // Block locked or deactivated accounts from updating reviews
        User user = userService.getUserById(userId);
        if (Boolean.TRUE.equals(user.getIsLocked())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account is locked and cannot update reviews.");
        }
        if (Boolean.TRUE.equals(user.getIsDeactivated())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account is deactivated and cannot update reviews.");
        }
        
        Review existingReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        
        // Validate the review belongs to the authenticated user
        if (!existingReview.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update your own reviews");
        }
        
        // Validate username matches
        if (username == null || !username.equals(updatedReview.getUserName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username in review must match authenticated user's username");
        }
        
        existingReview.setRating(updatedReview.getRating());
        existingReview.setTitle(updatedReview.getTitle());
        existingReview.setComment(updatedReview.getComment());
        
        log.info("Updating review: {} by user {}", reviewId, username);
        return reviewRepository.save(existingReview);
    }
    
    @Transactional
    public void deleteReview(Integer reviewId, String userId) {
        Review existingReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        
        // Validate the review belongs to the authenticated user
        if (!existingReview.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own reviews");
        }
        
        log.info("Deleting review: {} by user {}", reviewId, userId);
        reviewRepository.deleteById(reviewId);
    }
}
