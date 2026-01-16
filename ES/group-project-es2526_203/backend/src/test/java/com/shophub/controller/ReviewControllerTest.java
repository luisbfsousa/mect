package com.shophub.controller;

import com.shophub.model.Review;
import com.shophub.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private ReviewController controller;

    private Review sampleReview;
    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = "user-456";
        
        sampleReview = Review.builder()
                .reviewId(1)
                .productId(101)
                .userId(testUserId)
                .rating(5)
                .title("Great Product!")
                .comment("This product exceeded my expectations.")
                .userName("John Doe")
                .verifiedPurchase(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Test: Get all reviews for a product
     */
    @Test
    void getReviewsByProduct_ShouldReturnReviewList_WhenReviewsExist() {
        // Given
        Review review2 = Review.builder()
                .reviewId(2)
                .productId(101)
                .rating(4)
                .title("Good")
                .comment("Nice product")
                .userName("Jane Smith")
                .build();
        
        List<Review> reviews = Arrays.asList(sampleReview, review2);
        when(reviewService.getReviewsByProductId(101)).thenReturn(reviews);

        // When
        ResponseEntity<List<Review>> response = controller.getReviewsByProduct(101);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        assertEquals(5, response.getBody().get(0).getRating());
        assertEquals(4, response.getBody().get(1).getRating());
        
        verify(reviewService, times(1)).getReviewsByProductId(101);
    }

    /**
     * Test: Get review statistics for a product
     */
    @Test
    void getProductReviewStats_ShouldReturnStats() {
        // Given
        Map<String, Object> stats = new HashMap<>();
        stats.put("averageRating", 4.5);
        stats.put("totalReviews", 10);
        stats.put("ratingDistribution", Map.of(5, 6, 4, 3, 3, 1));
        
        when(reviewService.getProductReviewStats(101)).thenReturn(stats);

        // When
        ResponseEntity<Map<String, Object>> response = controller.getProductReviewStats(101);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(4.5, response.getBody().get("averageRating"));
        assertEquals(10, response.getBody().get("totalReviews"));
        
        verify(reviewService, times(1)).getProductReviewStats(101);
    }

    /**
     * Test: Get reviews by user
     */
    @Test
    void getReviewsByUser_ShouldReturnUserReviews() {
        // Given
        List<Review> userReviews = Arrays.asList(sampleReview);
        when(reviewService.getReviewsByUserId(testUserId)).thenReturn(userReviews);

        // When
        ResponseEntity<List<Review>> response = controller.getReviewsByUser(testUserId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(testUserId, response.getBody().get(0).getUserId());
        
        verify(reviewService, times(1)).getReviewsByUserId(testUserId);
    }

    /**
     * Test: Create a new review
     */
    @Test
    void createReview_ShouldReturnCreatedReview_WhenValidData() {
        // Given
        Review newReview = Review.builder()
                .productId(101)
                .rating(5)
                .title("Excellent")
                .comment("Highly recommend!")
                .build();
        
        when(jwt.getSubject()).thenReturn(testUserId);
        when(jwt.getClaimAsString("preferred_username")).thenReturn("johndoe");
        when(reviewService.createReview(any(Review.class), eq(testUserId), eq("johndoe"), anyList(), eq(jwt)))
                .thenReturn(sampleReview);

        // When
        ResponseEntity<Review> response = controller.createReview(jwt, newReview);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5, response.getBody().getRating());
        assertEquals("Great Product!", response.getBody().getTitle());
        
        verify(reviewService, times(1)).createReview(any(Review.class), eq(testUserId), eq("johndoe"), anyList(), eq(jwt));
    }

    /**
     * Test: Update an existing review
     */
    @Test
    void updateReview_ShouldReturnUpdatedReview_WhenValidData() {
        // Given
        Review updatedReview = Review.builder()
                .reviewId(1)
                .productId(101)
                .rating(4)
                .title("Updated Title")
                .comment("Updated comment")
                .build();
        
        when(jwt.getSubject()).thenReturn(testUserId);
        when(jwt.getClaimAsString("preferred_username")).thenReturn("johndoe");
        when(reviewService.updateReview(eq(1), any(Review.class), eq(testUserId), eq("johndoe"), anyList()))
                .thenReturn(updatedReview);

        // When
        ResponseEntity<Review> response = controller.updateReview(jwt, 1, updatedReview);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Updated Title", response.getBody().getTitle());
        assertEquals(4, response.getBody().getRating());
        
        verify(reviewService, times(1)).updateReview(eq(1), any(Review.class), eq(testUserId), eq("johndoe"), anyList());
    }

    /**
     * Test: Delete a review
     */
    @Test
    void deleteReview_ShouldReturnNoContent_WhenReviewDeleted() {
        // Given
        when(jwt.getSubject()).thenReturn(testUserId);
        doNothing().when(reviewService).deleteReview(1, testUserId);

        // When
        ResponseEntity<Void> response = controller.deleteReview(jwt, 1);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        
        verify(reviewService, times(1)).deleteReview(eq(1), eq(testUserId));
    }
}
