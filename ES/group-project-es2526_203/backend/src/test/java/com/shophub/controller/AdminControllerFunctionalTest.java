package com.shophub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shophub.config.TestSecurityConfig;
import com.shophub.dto.InventoryUpdateRequest;
import com.shophub.dto.ProductDTO;
import com.shophub.model.Category;
import com.shophub.model.Order;
import com.shophub.model.Product;
import com.shophub.model.User;
import com.shophub.repository.CategoryRepository;
import com.shophub.repository.OrderRepository;
import com.shophub.repository.ProductRepository;
import com.shophub.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class AdminControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void cleanup() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    @WithMockUser(roles = "administrator")
    void getAllProducts_asAdmin_returnsAllProducts() throws Exception {
        // Arrange
        productRepository.save(Product.builder()
                .name("Admin Product 1")
                .description("Description 1")
                .price(new BigDecimal("10.00"))
                .stockQuantity(10)
                .lowStockThreshold(2)
                .build());

        productRepository.save(Product.builder()
                .name("Admin Product 2")
                .description("Description 2")
                .price(new BigDecimal("20.00"))
                .stockQuantity(20)
                .lowStockThreshold(5)
                .build());

        // Act and Assert
        mockMvc.perform(get("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Admin Product 1")))
                .andExpect(jsonPath("$[1].name", is("Admin Product 2")));
    }

    @Test
    @Transactional
    @WithMockUser(roles = "administrator")
    void getProductById_asAdmin_returnsProduct() throws Exception {
        // Arrange
        Product product = productRepository.save(Product.builder()
                .name("Admin Product")
                .description("Admin Description")
                .price(new BigDecimal("99.99"))
                .stockQuantity(50)
                .lowStockThreshold(10)
                .build());

        // Act and Assert
        mockMvc.perform(get("/api/admin/products/{id}", product.getProductId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product_id", is(product.getProductId())))
                .andExpect(jsonPath("$.name", is("Admin Product")))
                .andExpect(jsonPath("$.price", is(99.99)));
    }

    @Test
    @Transactional
    @WithMockUser(roles = "administrator")
    void createProduct_asAdmin_createsAndReturns201() throws Exception {
        // Arrange
        ProductDTO productDTO = ProductDTO.builder()
                .name("New Product")
                .description("New Description")
                .price(new BigDecimal("150.00"))
                .sku("NEW-SKU-001")
                .stockQuantity(30)
                .lowStockThreshold(5)
                .images(Arrays.asList("image1.jpg", "image2.jpg"))
                .build();

        // Act and Assert
        mockMvc.perform(post("/api/admin/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.product_id").exists())
                .andExpect(jsonPath("$.name", is("New Product")))
                .andExpect(jsonPath("$.description", is("New Description")))
                .andExpect(jsonPath("$.price", is(150.00)))
                .andExpect(jsonPath("$.sku", is("NEW-SKU-001")))
                .andExpect(jsonPath("$.stock_quantity", is(30)))
                .andExpect(jsonPath("$.images", hasSize(2)));
    }

    @Test
    @Transactional
    @WithMockUser(roles = "administrator")
    void createProduct_withCategory_associatesCategoryCorrectly() throws Exception {
        // Arrange
        Category category = categoryRepository.save(Category.builder()
                .name("Electronics")
                .description("Electronic devices")
                .build());

        ProductDTO productDTO = ProductDTO.builder()
                .name("Laptop")
                .description("Gaming laptop")
                .price(new BigDecimal("1299.99"))
                .categoryId(category.getCategoryId())
                .stockQuantity(15)
                .lowStockThreshold(3)
                .build();

        // Act and Assert
        mockMvc.perform(post("/api/admin/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Laptop")))
                .andExpect(jsonPath("$.category_id", is(category.getCategoryId())));
    }

    @Test
    @Transactional
    @WithMockUser(roles = "administrator")
    void createProduct_withInvalidCategory_returns404() throws Exception {
        // Arrange
        ProductDTO productDTO = ProductDTO.builder()
                .name("Product")
                .description("Description")
                .price(new BigDecimal("50.00"))
                .categoryId(99999) // Non-existent category
                .stockQuantity(10)
                .build();

        // Act and Assert
        mockMvc.perform(post("/api/admin/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    @WithMockUser(roles = "administrator")
    void updateProduct_asAdmin_updatesAndReturnsProduct() throws Exception {
        // Arrange
        Product existingProduct = productRepository.save(Product.builder()
                .name("Old Name")
                .description("Old Description")
                .price(new BigDecimal("50.00"))
                .stockQuantity(10)
                .lowStockThreshold(2)
                .build());

        ProductDTO updateDTO = ProductDTO.builder()
                .name("Updated Name")
                .description("Updated Description")
                .price(new BigDecimal("75.00"))
                .sku("UPDATED-SKU")
                .stockQuantity(20)
                .lowStockThreshold(5)
                .build();

        // Act and Assert
        mockMvc.perform(put("/api/admin/products/{id}", existingProduct.getProductId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product_id", is(existingProduct.getProductId())))
                .andExpect(jsonPath("$.name", is("Updated Name")))
                .andExpect(jsonPath("$.description", is("Updated Description")))
                .andExpect(jsonPath("$.price", is(75.00)))
                .andExpect(jsonPath("$.sku", is("UPDATED-SKU")))
                .andExpect(jsonPath("$.stock_quantity", is(20)));
    }

    @Test
    @Transactional
    @WithMockUser(roles = "administrator")
    void updateProduct_nonExistent_returns404() throws Exception {
        // Arrange
        ProductDTO updateDTO = ProductDTO.builder()
                .name("Updated Name")
                .price(new BigDecimal("100.00"))
                .stockQuantity(10)
                .build();

        // Act and Assert
        mockMvc.perform(put("/api/admin/products/{id}", 99999).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    @WithMockUser(roles = "administrator")
    void deleteProduct_asAdmin_deletesAndReturnsSuccessMessage() throws Exception {
        // Arrange
        Product product = productRepository.save(Product.builder()
                .name("Product to Delete")
                .price(new BigDecimal("25.00"))
                .stockQuantity(5)
                .lowStockThreshold(1)
                .build());

        // Act and Assert
        mockMvc.perform(delete("/api/admin/products/{id}", product.getProductId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Product deleted successfully")));

        // Verify product is actually deleted
        mockMvc.perform(get("/api/admin/products/{id}", product.getProductId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    @WithMockUser(roles = "administrator")
    void deleteProduct_nonExistent_returns404() throws Exception {
        // Act and Assert
        mockMvc.perform(delete("/api/admin/products/{id}", 99999).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    @WithMockUser(roles = "administrator")
    void getAllOrders_asAdmin_returnsAllOrders() throws Exception {
        // Arrange - create users and orders
        User user1 = userRepository.save(User.builder()
                .userId("user-1")
                .email("user1@example.com")
                .firstName("User")
                .lastName("One")
                .role("customer")
                .build());

        User user2 = userRepository.save(User.builder()
                .userId("user-2")
                .email("user2@example.com")
                .firstName("User")
                .lastName("Two")
                .role("customer")
                .build());

        orderRepository.save(Order.builder()
                .userId("user-1")
                .orderStatus("pending")
                .totalAmount(new BigDecimal("100.00"))
                .build());

        orderRepository.save(Order.builder()
                .userId("user-2")
                .orderStatus("shipped")
                .totalAmount(new BigDecimal("200.00"))
                .build());

        // Act and Assert
        mockMvc.perform(get("/api/admin/orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].user_id", containsInAnyOrder("user-1", "user-2")));
    }

    @Test
    @Transactional
    @WithMockUser(roles = "administrator")
    void getAllOrders_withStatusFilter_returnsFilteredOrders() throws Exception {
        // Arrange
        orderRepository.save(Order.builder()
                .userId("user-1")
                .orderStatus("pending")
                .totalAmount(new BigDecimal("100.00"))
                .build());

        orderRepository.save(Order.builder()
                .userId("user-2")
                .orderStatus("shipped")
                .totalAmount(new BigDecimal("200.00"))
                .build());

        orderRepository.save(Order.builder()
                .userId("user-3")
                .orderStatus("pending")
                .totalAmount(new BigDecimal("150.00"))
                .build());

        // Act and Assert - filter by pending status
        mockMvc.perform(get("/api/admin/orders")
                        .param("status", "pending")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].order_status", everyItem(is("pending"))));
    }

    @Test
    @Transactional
    @WithMockUser(roles = "administrator")
    void getAllOrders_withSearchFilter_returnsMatchingOrders() throws Exception {
        // Arrange
        orderRepository.save(Order.builder()
                .userId("user-abc")
                .orderStatus("pending")
                .totalAmount(new BigDecimal("100.00"))
                .build());

        orderRepository.save(Order.builder()
                .userId("user-xyz")
                .orderStatus("shipped")
                .totalAmount(new BigDecimal("200.00"))
                .build());

        // Act and Assert - search by userId substring
        mockMvc.perform(get("/api/admin/orders")
                        .param("search", "abc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].user_id", containsString("abc")));
    }

    @Test
    @Transactional
    @WithMockUser(roles = "administrator")
    void getOrderStatistics_asAdmin_returnsStatisticsPlaceholder() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/admin/orders/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Order statistics endpoint")))
                .andExpect(jsonPath("$.status", is("not_implemented")));
    }

    @Test
    @Transactional
    void getAllProducts_withoutAuthentication_returns401or403() throws Exception {
        // Act and Assert - no authentication (can be 401 or 403 depending on config)
        mockMvc.perform(get("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but got: " + status);
                    }
                });
    }

    @Test
    @Transactional
    @WithMockUser(roles = "CUSTOMER")
    void getAllProducts_asCustomer_returns403() throws Exception {
        // Act and Assert - customer role should be denied
        mockMvc.perform(get("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @WithMockUser(roles = "administrator")
    void createProduct_withInvalidData_returns400() throws Exception {
        // Arrange - missing required fields
        ProductDTO invalidDTO = ProductDTO.builder()
                .description("Only description")
                // Missing name and price
                .build();

        // Act and Assert
        mockMvc.perform(post("/api/admin/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @Transactional
    @WithMockUser(roles = "administrator")
    void updateInventory_asAdmin_updatesStockSuccessfully() throws Exception {
        // Arrange
        Product product = productRepository.save(Product.builder()
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("50.00"))
                .stockQuantity(10)
                .lowStockThreshold(2)
                .build());

        InventoryUpdateRequest updateRequest = InventoryUpdateRequest.builder()
                .stockQuantity(25)
                .build();

        // Act and Assert
        mockMvc.perform(put("/api/admin/products/{id}/inventory", product.getProductId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product_id", is(product.getProductId())))
                .andExpect(jsonPath("$.stock_quantity", is(25)));

        // Verify in database
        Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();
        assert updatedProduct.getStockQuantity() == 25;
    }
    
    @Test
    @Transactional
    @WithMockUser(roles = "administrator")
    void updateInventory_withNegativeStock_returns400() throws Exception {
        // Arrange
        Product product = productRepository.save(Product.builder()
                .name("Test Product")
                .price(new BigDecimal("50.00"))
                .stockQuantity(10)
                .build());

        InventoryUpdateRequest updateRequest = InventoryUpdateRequest.builder()
                .stockQuantity(-5)
                .build();

        // Act and Assert
        mockMvc.perform(put("/api/admin/products/{id}/inventory", product.getProductId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @Transactional
    @WithMockUser(roles = "administrator")
    void updateInventory_nonExistentProduct_returns404() throws Exception {
        // Arrange
        InventoryUpdateRequest updateRequest = InventoryUpdateRequest.builder()
                .stockQuantity(50)
                .build();

        // Act and Assert
        mockMvc.perform(put("/api/admin/products/{id}/inventory", 99999).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @Transactional
    @WithMockUser(roles = "CUSTOMER")
    void updateInventory_asCustomer_returns403() throws Exception {
        // Arrange
        Product product = productRepository.save(Product.builder()
                .name("Test Product")
                .price(new BigDecimal("50.00"))
                .stockQuantity(10)
                .build());

        InventoryUpdateRequest updateRequest = InventoryUpdateRequest.builder()
                .stockQuantity(20)
                .build();

        // Act and Assert - customer role should be denied
        mockMvc.perform(put("/api/admin/products/{id}/inventory", product.getProductId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }
}
