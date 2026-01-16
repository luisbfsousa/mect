package com.shophub.repository;

import com.shophub.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    
    // Find all reviews for a specific product
    List<Review> findByProductIdOrderByCreatedAtDesc(Integer productId);
    
    // Find reviews by user
    List<Review> findByUserIdOrderByCreatedAtDesc(String userId);
    
    // Check if user already reviewed a product
    boolean existsByUserIdAndProductId(String userId, Integer productId);
    
    // Get average rating for a product
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.productId = :productId")
    Double getAverageRatingByProductId(@Param("productId") Integer productId);
    
    // Get review count for a product
    @Query("SELECT COUNT(r) FROM Review r WHERE r.productId = :productId")
    Long getReviewCountByProductId(@Param("productId") Integer productId);
}
