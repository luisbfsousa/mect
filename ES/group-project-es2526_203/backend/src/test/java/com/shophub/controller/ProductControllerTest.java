package com.shophub.controller;

import com.shophub.model.Product;
import com.shophub.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Test class for ProductController - Browse Product Catalog functionality
 */
@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private Product sampleProduct1;
    private Product sampleProduct2;
    private List<Product> productList;

    @BeforeEach
    void setUp() {
        sampleProduct1 = Product.builder()
                .productId(1)
                .name("Laptop")
                .description("High-performance laptop")
                .price(new BigDecimal("1299.99"))
                .stockQuantity(15)
                .categoryId(1)
                .categoryName("Electronics")
                .averageRating(4.5)
                .reviewCount(25L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleProduct2 = Product.builder()
                .productId(2)
                .name("Wireless Mouse")
                .description("Ergonomic wireless mouse")
                .price(new BigDecimal("99.99"))
                .stockQuantity(50)
                .categoryId(1)
                .categoryName("Electronics")
                .averageRating(4.8)
                .reviewCount(120L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        productList = Arrays.asList(sampleProduct1, sampleProduct2);
    }

    /**
     * Test: Browse all products in the catalog
     */
    @Test
    void getAllProducts_ShouldReturnAllProducts_WhenCatalogIsNotEmpty() {
        // Given
        when(productService.getAllProducts()).thenReturn(productList);

        // When
        ResponseEntity<List<Product>> response = productController.getAllProducts();

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals(2, response.getBody().size(), "Should return 2 products");
        
        // Verify product details
        Product firstProduct = response.getBody().get(0);
        assertEquals("Laptop", firstProduct.getName());
        assertEquals(new BigDecimal("1299.99"), firstProduct.getPrice());
        assertEquals(15, firstProduct.getStockQuantity());
        assertEquals("Electronics", firstProduct.getCategoryName());
        
        verify(productService, times(1)).getAllProducts();
    }

    /**
     * Test: Browse empty product catalog
     */
    @Test
    void getAllProducts_ShouldReturnEmptyList_WhenCatalogIsEmpty() {
        // Given
        when(productService.getAllProducts()).thenReturn(Arrays.asList());

        // When
        ResponseEntity<List<Product>> response = productController.getAllProducts();

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals(0, response.getBody().size(), "Should return empty list");
        
        verify(productService, times(1)).getAllProducts();
    }

    /**
     * Test: Browse product by ID
     */
    @Test
    void getProductById_ShouldReturnProduct_WhenProductExists() {
        // Given
        when(productService.getProductById(1)).thenReturn(sampleProduct1);

        // When
        ResponseEntity<Product> response = productController.getProductById(1);

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        
        Product product = response.getBody();
        assertEquals(1, product.getProductId());
        assertEquals("Laptop", product.getName());
        assertEquals(new BigDecimal("1299.99"), product.getPrice());
        assertEquals("High-performance laptop", product.getDescription());
        assertEquals(4.5, product.getAverageRating());
        assertEquals(25L, product.getReviewCount());
        
        verify(productService, times(1)).getProductById(1);
    }

    /**
     * Test: Browse products by category
     */
    @Test
    void getProductsByCategory_ShouldReturnFilteredProducts_WhenCategoryExists() {
        // Given
        when(productService.getProductsByCategory(1)).thenReturn(productList);

        // When
        ResponseEntity<List<Product>> response = productController.getProductsByCategory(1);

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        assertEquals(2, response.getBody().size(), "Should return 2 products in Electronics category");
        
        // Verify all products belong to the same category
        response.getBody().forEach(product -> {
            assertEquals("Electronics", product.getCategoryName(), 
                "All products should belong to Electronics category");
        });
        
        verify(productService, times(1)).getProductsByCategory(1);
    }

    /**
     * Test: Verify product catalog contains essential information
     */
    @Test
    void getAllProducts_ShouldContainEssentialProductInformation() {
        // Given
        when(productService.getAllProducts()).thenReturn(productList);

        // When
        ResponseEntity<List<Product>> response = productController.getAllProducts();

        // Then
        assertNotNull(response.getBody(), "Product list should not be null");
        
        response.getBody().forEach(product -> {
            assertNotNull(product.getProductId(), "Product ID should not be null");
            assertNotNull(product.getName(), "Product name should not be null");
            assertNotNull(product.getPrice(), "Product price should not be null");
            assertNotNull(product.getStockQuantity(), "Stock quantity should not be null");
            assertNotNull(product.getCategoryName(), "Category name should not be null");
            assertNotNull(product.getAverageRating(), "Average rating should not be null");
            assertNotNull(product.getReviewCount(), "Review count should not be null");
            
            // Verify price is positive
            assertTrue(product.getPrice().compareTo(BigDecimal.ZERO) > 0, 
                "Product price should be positive");
            
            // Verify stock quantity is non-negative
            assertTrue(product.getStockQuantity() >= 0, 
                "Stock quantity should be non-negative");
        });
        
        verify(productService, times(1)).getAllProducts();
    }
}
