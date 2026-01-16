package com.shophub.repository;

import com.shophub.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    
    @Query("SELECT p FROM Product p ORDER BY p.createdAt DESC")
    List<Product> findAllOrderByCreatedAtDesc();
    
    @Query("SELECT p FROM Product p WHERE p.categoryId = :categoryId")
    List<Product> findByCategoryId(Integer categoryId);
    
    // Query with category join
    @Query(value = "SELECT p.*, c.name as category_name FROM products p " +
           "LEFT JOIN categories c ON p.category_id = c.category_id " +
           "ORDER BY p.created_at DESC", nativeQuery = true)
    List<Product> findAllWithCategoryName();
}