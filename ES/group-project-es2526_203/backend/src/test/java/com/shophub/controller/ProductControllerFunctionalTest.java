package com.shophub.controller;

import com.shophub.config.TestSecurityConfig;
import com.shophub.model.Category;
import com.shophub.model.Product;
import com.shophub.repository.CategoryRepository;
import com.shophub.repository.ProductRepository;
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
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class ProductControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category category1;
    private Category category2;
    private Product product1;
    private Product product2;
    private Product product3;

    @BeforeEach
    void setUp() {
        // Clean up
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        // Create test categories
        category1 = Category.builder()
                .name("Electronics")
                .description("Electronic items")
                .build();
        category1 = categoryRepository.save(category1);

        category2 = Category.builder()
                .name("Clothing")
                .description("Clothing items")
                .build();
        category2 = categoryRepository.save(category2);

        // Create test products
        product1 = Product.builder()
                .name("Laptop")
                .description("Gaming Laptop")
                .price(new BigDecimal("999.99"))
                .stockQuantity(10)
                .categoryId(category1.getCategoryId())
                .sku("LAP-001")
                .images(Arrays.asList("image1.jpg", "image2.jpg"))
                .build();
        product1 = productRepository.save(product1);

        product2 = Product.builder()
                .name("Mouse")
                .description("Wireless Mouse")
                .price(new BigDecimal("29.99"))
                .stockQuantity(50)
                .categoryId(category1.getCategoryId())
                .sku("MOU-001")
                .build();
        product2 = productRepository.save(product2);

        product3 = Product.builder()
                .name("T-Shirt")
                .description("Cotton T-Shirt")
                .price(new BigDecimal("19.99"))
                .stockQuantity(100)
                .categoryId(category2.getCategoryId())
                .sku("TSH-001")
                .build();
        product3 = productRepository.save(product3);
    }

    @Test
    @Transactional
    void getAllProducts_returnsAllProducts() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].product_id", hasItem(product1.getProductId())))
                .andExpect(jsonPath("$[*].product_id", hasItem(product2.getProductId())))
                .andExpect(jsonPath("$[*].product_id", hasItem(product3.getProductId())));
    }

    @Test
    @Transactional
    void getAllProducts_returnsEmptyList_whenNoProducts() throws Exception {
        // Arrange
        productRepository.deleteAll();

        // Act and Assert
        mockMvc.perform(get("/api/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Transactional
    void getAllProducts_isPublicEndpoint_noAuthRequired() throws Exception {
        // Act and Assert - should work without authentication
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @Transactional
    void getAllProducts_returnsProductsWithAllFields() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].product_id").exists())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].description").exists())
                .andExpect(jsonPath("$[0].price").exists())
                .andExpect(jsonPath("$[0].stock_quantity").exists())
                .andExpect(jsonPath("$[0].category_id").exists());
    }

    @Test
    @Transactional
    void getProductById_returnsProduct_whenProductExists() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/products/" + product1.getProductId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product_id", is(product1.getProductId())))
                .andExpect(jsonPath("$.name", is("Laptop")))
                .andExpect(jsonPath("$.description", is("Gaming Laptop")))
                .andExpect(jsonPath("$.price", is(999.99)))
                .andExpect(jsonPath("$.stock_quantity", is(10)))
                .andExpect(jsonPath("$.category_id", is(category1.getCategoryId())))
                .andExpect(jsonPath("$.sku", is("LAP-001")));
    }

    @Test
    @Transactional
    void getProductById_returnsProductWithImages_whenImagesExist() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/products/" + product1.getProductId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images", hasSize(2)))
                .andExpect(jsonPath("$.images[0]", is("image1.jpg")))
                .andExpect(jsonPath("$.images[1]", is("image2.jpg")));
    }

    @Test
    @Transactional
    void getProductById_returns404_whenProductDoesNotExist() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/products/99999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void getProductById_isPublicEndpoint_noAuthRequired() throws Exception {
        // Act and Assert - should work without authentication
        mockMvc.perform(get("/api/products/" + product1.getProductId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product_id", is(product1.getProductId())));
    }

    @Test
    @Transactional
    void getProductsByCategory_returnsProducts_whenCategoryHasProducts() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/products/category/" + category1.getCategoryId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].category_id", everyItem(is(category1.getCategoryId()))))
                .andExpect(jsonPath("$[*].name", hasItems("Laptop", "Mouse")));
    }

    @Test
    @Transactional
    void getProductsByCategory_returnsOnlyProductsFromSpecifiedCategory() throws Exception {
        // Act and Assert - Get products from category2
        mockMvc.perform(get("/api/products/category/" + category2.getCategoryId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].category_id", is(category2.getCategoryId())))
                .andExpect(jsonPath("$[0].name", is("T-Shirt")));
    }

    @Test
    @Transactional
    void getProductsByCategory_returnsEmptyList_whenCategoryHasNoProducts() throws Exception {
        // Arrange - Create a new category with no products
        Category emptyCategory = Category.builder()
                .name("Empty Category")
                .description("No products here")
                .build();
        emptyCategory = categoryRepository.save(emptyCategory);

        // Act and Assert
        mockMvc.perform(get("/api/products/category/" + emptyCategory.getCategoryId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Transactional
    void getProductsByCategory_returnsEmptyList_whenCategoryDoesNotExist() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/products/category/99999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Transactional
    void getProductsByCategory_isPublicEndpoint_noAuthRequired() throws Exception {
        // Act and Assert - should work without authentication
        mockMvc.perform(get("/api/products/category/" + category1.getCategoryId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @Transactional
    void getProductsByCategory_returnsProductsWithCorrectPriceFormat() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/products/category/" + category1.getCategoryId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].price").isNumber())
                .andExpect(jsonPath("$[1].price").isNumber());
    }

    @Test
    @Transactional
    void getAllProducts_withMultipleRequests_returnsConsistentResults() throws Exception {
        // Act and Assert - Multiple requests should return same data
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/products")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)));
        }
    }
}
