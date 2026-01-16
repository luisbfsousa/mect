package com.shophub.repository;

import com.shophub.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    
    List<Cart> findByUserId(String userId);
    
    Optional<Cart> findByUserIdAndProductId(String userId, Integer productId);
    
    @Modifying
    @Query("DELETE FROM Cart c WHERE c.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);
}