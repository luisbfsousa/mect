package com.shophub.service;

import com.shophub.dto.ProductDTO;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.model.Category;
import com.shophub.model.Product;
import com.shophub.repository.CategoryRepository;
import com.shophub.repository.ProductRepository;
import com.shophub.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private Category testCategory;
    private ProductDTO testProductDTO;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .categoryId(1)
                .name("Electronics")
                .build();

        testProduct = Product.builder()
                .productId(1)
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .sku("TEST-001")
                .categoryId(1)
                .stockQuantity(10)
                .lowStockThreshold(5)
                .build();

        testProductDTO = ProductDTO.builder()
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .sku("TEST-001")
                .categoryId(1)
                .stockQuantity(10)
                .lowStockThreshold(5)
                .build();
    }

    @Test
    void getAllProducts_ShouldReturnAllProductsWithCategoryNames() {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        when(productRepository.findAll()).thenReturn(products);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(testCategory));
        when(reviewRepository.getAverageRatingByProductId(1)).thenReturn(4.5);
        when(reviewRepository.getReviewCountByProductId(1)).thenReturn(10L);

        // When
        List<Product> result = productService.getAllProducts();

        // Then
        assertEquals(1, result.size());
        assertEquals("Test Product", result.get(0).getName());
        assertEquals("Electronics", result.get(0).getCategoryName());
        verify(productRepository).findAll();
        verify(categoryRepository).findById(1);
    }

    @Test
    void getProductById_ShouldReturnProductWithCategoryName() {
        // Given
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(testCategory));
        when(reviewRepository.getAverageRatingByProductId(1)).thenReturn(4.5);
        when(reviewRepository.getReviewCountByProductId(1)).thenReturn(10L);

        // When
        Product result = productService.getProductById(1);

        // Then
        assertEquals("Test Product", result.getName());
        assertEquals("Electronics", result.getCategoryName());
        verify(productRepository).findById(1);
        verify(categoryRepository).findById(1);
    }

    @Test
    void getProductById_ShouldThrowExceptionWhenProductNotFound() {
        // Given
        when(productRepository.findById(1)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(1));
        verify(productRepository).findById(1);
    }

    @Test
    void getProductsByCategory_ShouldReturnProductsWithCategoryNames() {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        when(productRepository.findByCategoryId(1)).thenReturn(products);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(testCategory));

        // When
        List<Product> result = productService.getProductsByCategory(1);

        // Then
        assertEquals(1, result.size());
        assertEquals("Electronics", result.get(0).getCategoryName());
        verify(productRepository).findByCategoryId(1);
        verify(categoryRepository).findById(1);
    }

    @Test
    void createProduct_ShouldCreateProductSuccessfully() {
        // Given
        when(categoryRepository.findById(1)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        Product result = productService.createProduct(testProductDTO);

        // Then
        assertEquals("Test Product", result.getName());
        verify(categoryRepository).findById(1);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_ShouldThrowExceptionWhenCategoryNotFound() {
        // Given
        when(categoryRepository.findById(1)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> productService.createProduct(testProductDTO));
        verify(categoryRepository).findById(1);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProduct_ShouldUpdateProductSuccessfully() {
        // Given
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        Product result = productService.updateProduct(1, testProductDTO);

        // Then
        assertEquals("Test Product", result.getName());
        verify(productRepository).findById(1);
        verify(categoryRepository).findById(1);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_ShouldThrowExceptionWhenProductNotFound() {
        // Given
        when(productRepository.findById(1)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> productService.updateProduct(1, testProductDTO));
        verify(productRepository).findById(1);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_ShouldDeleteProductSuccessfully() {
        // Given
        when(productRepository.existsById(1)).thenReturn(true);

        // When
        productService.deleteProduct(1);

        // Then
        verify(productRepository).existsById(1);
        verify(productRepository).deleteById(1);
    }

    @Test
    void deleteProduct_ShouldThrowExceptionWhenProductNotFound() {
        // Given
        when(productRepository.existsById(1)).thenReturn(false);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> productService.deleteProduct(1));
        verify(productRepository).existsById(1);
        verify(productRepository, never()).deleteById(anyInt());
    }

    @Test
    void updateStock_ShouldUpdateStockSuccessfully() {
        // Given
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        productService.updateStock(1, 3);

        // Then
        assertEquals(7, testProduct.getStockQuantity());
        verify(productRepository).findById(1);
        verify(productRepository).save(testProduct);
    }

    @Test
    void updateStock_ShouldThrowExceptionWhenInsufficientStock() {
        // Given
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> productService.updateStock(1, 15));
        verify(productRepository).findById(1);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateStock_ShouldThrowExceptionWhenProductNotFound() {
        // Given
        when(productRepository.findById(1)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> productService.updateStock(1, 3));
        verify(productRepository).findById(1);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void setStockQuantity_ShouldSetStockSuccessfully() {
        // Given
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        Product result = productService.setStockQuantity(1, 25);

        // Then
        assertEquals(25, result.getStockQuantity());
        verify(productRepository).findById(1);
        verify(productRepository).save(testProduct);
    }

    @Test
    void setStockQuantity_ShouldThrowExceptionWhenProductNotFound() {
        // Given
        when(productRepository.findById(1)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> productService.setStockQuantity(1, 25));
        verify(productRepository).findById(1);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void setStockQuantity_ShouldAcceptZeroStock() {
        // Given
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        Product result = productService.setStockQuantity(1, 0);

        // Then
        assertEquals(0, result.getStockQuantity());
        verify(productRepository).findById(1);
        verify(productRepository).save(testProduct);
    }
}
