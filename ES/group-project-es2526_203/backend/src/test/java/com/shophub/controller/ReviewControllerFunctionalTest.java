package com.shophub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shophub.config.TestSecurityConfig;
import com.shophub.model.Category;
import com.shophub.model.Product;
import com.shophub.model.Review;
import com.shophub.repository.CategoryRepository;
import com.shophub.repository.ProductRepository;
import com.shophub.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class ReviewControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Product testProduct;
    private String testUserId = "test-user-123";
    private String otherUserId = "other-user-456";

    @BeforeEach
    void setUp() {
        // Clean up
        reviewRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        // Create test category
        Category category = Category.builder()
                .name("Electronics")
                .description("Electronic items")
                .build();
        category = categoryRepository.save(category);

        // Create test product
        testProduct = Product.builder()
                .name("Laptop")
                .description("Gaming Laptop")
                .price(new BigDecimal("999.99"))
                .stockQuantity(10)
                .categoryId(category.getCategoryId())
                .build();
        testProduct = productRepository.save(testProduct);
    }

    @Test
    @Transactional
    void getReviewsByProduct_returnsEmptyList_whenNoReviews() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/reviews/product/" + testProduct.getProductId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Transactional
    void getReviewsByProduct_returnsReviews_whenReviewsExist() throws Exception {
        // Arrange
        Review review1 = Review.builder()
                .productId(testProduct.getProductId())
                .userId(testUserId)
                .userName("John Doe")
                .rating(5)
                .title("Great product!")
                .comment("I love this laptop!")
                .verifiedPurchase(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        reviewRepository.save(review1);

        Review review2 = Review.builder()
                .productId(testProduct.getProductId())
                .userId(otherUserId)
                .userName("Jane Smith")
                .rating(4)
                .title("Good value")
                .comment("Nice laptop for the price")
                .verifiedPurchase(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        reviewRepository.save(review2);

        // Act and Assert
        mockMvc.perform(get("/api/reviews/product/" + testProduct.getProductId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].product_id", everyItem(is(testProduct.getProductId()))));
    }

    @Test
    @Transactional
    void getReviewsByProduct_isPublicEndpoint_noAuthRequired() throws Exception {
        // Arrange
        Review review = Review.builder()
                .productId(testProduct.getProductId())
                .userId(testUserId)
                .userName("John Doe")
                .rating(5)
                .title("Great!")
                .comment("Excellent product")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        reviewRepository.save(review);

        // Act and Assert - should work without authentication
        mockMvc.perform(get("/api/reviews/product/" + testProduct.getProductId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @Transactional
    void getProductReviewStats_returnsStats_whenReviewsExist() throws Exception {
        // Arrange
        Review review1 = Review.builder()
                .productId(testProduct.getProductId())
                .userId(testUserId)
                .userName("User1")
                .rating(5)
                .title("Title")
                .comment("Comment")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        reviewRepository.save(review1);

        Review review2 = Review.builder()
                .productId(testProduct.getProductId())
                .userId(otherUserId)
                .userName("User2")
                .rating(3)
                .title("Title2")
                .comment("Comment2")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        reviewRepository.save(review2);

        // Act and Assert
        mockMvc.perform(get("/api/reviews/product/" + testProduct.getProductId() + "/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").exists())
                .andExpect(jsonPath("$.reviewCount").exists());
    }

    @Test
    @Transactional
    void getProductReviewStats_isPublicEndpoint_noAuthRequired() throws Exception {
        // Act and Assert - should work without authentication
        mockMvc.perform(get("/api/reviews/product/" + testProduct.getProductId() + "/stats"))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void getReviewsByUser_returnsUserReviews() throws Exception {
        // Arrange
        Review review1 = Review.builder()
                .productId(testProduct.getProductId())
                .userId(testUserId)
                .userName("John Doe")
                .rating(5)
                .title("Great!")
                .comment("Love it!")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        reviewRepository.save(review1);

        Review review2 = Review.builder()
                .productId(testProduct.getProductId())
                .userId(otherUserId)
                .userName("Jane Smith")
                .rating(4)
                .title("Good")
                .comment("Nice")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        reviewRepository.save(review2);

        // Act and Assert
        mockMvc.perform(get("/api/reviews/user/" + testUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].user_id", is(testUserId)));
    }

    @Test
    @Transactional
    void getReviewsByUser_returnsEmptyList_whenUserHasNoReviews() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/reviews/user/" + testUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Transactional
    void getReviewsByUser_isPublicEndpoint_noAuthRequired() throws Exception {
        // Act and Assert - should work without authentication
        mockMvc.perform(get("/api/reviews/user/" + testUserId))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void createReview_createsReview_withValidData() throws Exception {
        // Arrange
        Review review = Review.builder()
                .productId(testProduct.getProductId())
                .userName("johndoe")
                .rating(5)
                .title("Excellent product!")
                .comment("This laptop exceeded my expectations. Great performance!")
                .build();

        String reviewJson = objectMapper.writeValueAsString(review);

        // Act and Assert
        mockMvc.perform(post("/api/reviews")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId)
                                .claim("preferred_username", "johndoe")
                                .claim("realm_access", Map.of("roles", java.util.List.of("customer")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.review_id").exists())
                .andExpect(jsonPath("$.product_id", is(testProduct.getProductId())))
                .andExpect(jsonPath("$.user_id", is(testUserId)))
                .andExpect(jsonPath("$.rating", is(5)))
                .andExpect(jsonPath("$.title", is("Excellent product!")))
                .andExpect(jsonPath("$.comment", is("This laptop exceeded my expectations. Great performance!")));
    }



    @Test
    @Transactional
    void updateReview_updatesReview_whenUserOwnsReview() throws Exception {
        // Arrange - Create a review
        Review existingReview = Review.builder()
                .productId(testProduct.getProductId())
                .userId(testUserId)
                .userName("johndoe")
                .rating(3)
                .title("Okay product")
                .comment("It's okay")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        existingReview = reviewRepository.save(existingReview);

        // Update data
        Review updatedData = Review.builder()
                .productId(testProduct.getProductId())
                .userName("johndoe")
                .rating(5)
                .title("Actually great!")
                .comment("Changed my mind, this is excellent!")
                .build();

        String reviewJson = objectMapper.writeValueAsString(updatedData);

        // Act and Assert
        mockMvc.perform(put("/api/reviews/" + existingReview.getReviewId())
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId)
                                .claim("preferred_username", "johndoe")
                                .claim("realm_access", Map.of("roles", java.util.List.of("customer")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.review_id", is(existingReview.getReviewId())))
                .andExpect(jsonPath("$.rating", is(5)))
                .andExpect(jsonPath("$.title", is("Actually great!")))
                .andExpect(jsonPath("$.comment", is("Changed my mind, this is excellent!")));
    }

    @Test
    @Transactional
    void updateReview_returns404_whenReviewDoesNotExist() throws Exception {
        // Arrange
        Review updatedData = Review.builder()
                .productId(testProduct.getProductId())
                .userName("johndoe")
                .rating(5)
                .title("Great!")
                .comment("Excellent!")
                .build();

        String reviewJson = objectMapper.writeValueAsString(updatedData);

        // Act and Assert
        mockMvc.perform(put("/api/reviews/99999")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId)
                                .claim("preferred_username", "johndoe")
                                .claim("realm_access", Map.of("roles", java.util.List.of("customer")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson))
                .andExpect(status().isNotFound());
    }



    @Test
    @Transactional
    void deleteReview_deletesReview_whenUserOwnsReview() throws Exception {
        // Arrange
        Review review = Review.builder()
                .productId(testProduct.getProductId())
                .userId(testUserId)
                .userName("John Doe")
                .rating(5)
                .title("Great!")
                .comment("Love it!")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        review = reviewRepository.save(review);

        // Act and Assert
        mockMvc.perform(delete("/api/reviews/" + review.getReviewId())
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId))))
                .andExpect(status().isNoContent());

        // Verify review was deleted
        mockMvc.perform(get("/api/reviews/product/" + testProduct.getProductId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Transactional
    void deleteReview_returns404_whenReviewDoesNotExist() throws Exception {
        // Act and Assert
        mockMvc.perform(delete("/api/reviews/99999")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId))))
                .andExpect(status().isNotFound());
    }



    @Test
    @Transactional
    void getReviewsByProduct_returnsReviewsWithAllFields() throws Exception {
        // Arrange
        Review review = Review.builder()
                .productId(testProduct.getProductId())
                .userId(testUserId)
                .userName("John Doe")
                .rating(5)
                .title("Excellent!")
                .comment("Great laptop!")
                .verifiedPurchase(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        reviewRepository.save(review);

        // Act and Assert
        mockMvc.perform(get("/api/reviews/product/" + testProduct.getProductId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].review_id").exists())
                .andExpect(jsonPath("$[0].product_id").exists())
                .andExpect(jsonPath("$[0].user_id").exists())
                .andExpect(jsonPath("$[0].rating").exists())
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].comment").exists())
                .andExpect(jsonPath("$[0].user_name").exists())
                .andExpect(jsonPath("$[0].verified_purchase").exists())
                .andExpect(jsonPath("$[0].created_at").exists())
                .andExpect(jsonPath("$[0].updated_at").exists());
    }

    @Test
    @Transactional
    void getReviewsByProduct_onlyReturnsReviewsForSpecifiedProduct() throws Exception {
        // Arrange - Create another product
        Product anotherProduct = Product.builder()
                .name("Mouse")
                .description("Wireless Mouse")
                .price(new BigDecimal("29.99"))
                .stockQuantity(50)
                .categoryId(testProduct.getCategoryId())
                .build();
        anotherProduct = productRepository.save(anotherProduct);

        // Create reviews for both products
        Review review1 = Review.builder()
                .productId(testProduct.getProductId())
                .userId(testUserId)
                .userName("User1")
                .rating(5)
                .title("Title1")
                .comment("Comment1")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        reviewRepository.save(review1);

        Review review2 = Review.builder()
                .productId(anotherProduct.getProductId())
                .userId(testUserId)
                .userName("User1")
                .rating(4)
                .title("Title2")
                .comment("Comment2")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        reviewRepository.save(review2);

        // Act and Assert - Get reviews only for testProduct
        mockMvc.perform(get("/api/reviews/product/" + testProduct.getProductId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].product_id", is(testProduct.getProductId())));
    }
}
