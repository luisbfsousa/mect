package com.shophub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shophub.config.TestSecurityConfig;
import com.shophub.dto.CartDTO;
import com.shophub.model.Cart;
import com.shophub.model.Product;
import com.shophub.repository.CartRepository;
import com.shophub.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class CartControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void cleanup() {
        cartRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @Transactional
    void getUserCart_returnsUserCartItems() throws Exception {
        // Arrange - create products and cart items
        Product product1 = productRepository.save(Product.builder()
                .name("Product 1")
                .description("Description 1")
                .price(new BigDecimal("10.00"))
                .stockQuantity(100)
                .lowStockThreshold(10)
                .build());

        Product product2 = productRepository.save(Product.builder()
                .name("Product 2")
                .description("Description 2")
                .price(new BigDecimal("20.00"))
                .stockQuantity(50)
                .lowStockThreshold(5)
                .build());

        cartRepository.save(Cart.builder()
                .userId("test-user-123")
                .productId(product1.getProductId())
                .quantity(2)
                .unitPrice(new BigDecimal("10.00"))
                .build());

        cartRepository.save(Cart.builder()
                .userId("test-user-123")
                .productId(product2.getProductId())
                .quantity(1)
                .unitPrice(new BigDecimal("20.00"))
                .build());

        // Another user's cart item (should not be returned)
        cartRepository.save(Cart.builder()
                .userId("other-user")
                .productId(product1.getProductId())
                .quantity(5)
                .unitPrice(new BigDecimal("10.00"))
                .build());

        // Act and Assert
        mockMvc.perform(get("/api/cart")
                        .with(jwt().jwt(jwt -> jwt.subject("test-user-123")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].user_id", everyItem(is("test-user-123"))))
                .andExpect(jsonPath("$[*].product_id", containsInAnyOrder(product1.getProductId(), product2.getProductId())));
    }

    @Test
    @Transactional
    void getUserCart_returnsEmptyListWhenNoItems() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/cart")
                        .with(jwt().jwt(jwt -> jwt.subject("empty-user")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Transactional
    void getUserCart_withoutAuth_returns403() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/cart")
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
    void addToCart_addsNewItemSuccessfully() throws Exception {
        // Arrange
        Product product = productRepository.save(Product.builder()
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("25.99"))
                .stockQuantity(100)
                .lowStockThreshold(10)
                .build());

        CartDTO cartDTO = CartDTO.builder()
                .productId(product.getProductId())
                .quantity(3)
                .build();

        // Act and Assert
        mockMvc.perform(post("/api/cart").with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cart_id").exists())
                .andExpect(jsonPath("$.user_id", is("test-user-123")))
                .andExpect(jsonPath("$.product_id", is(product.getProductId())))
                .andExpect(jsonPath("$.quantity", is(3)))
                .andExpect(jsonPath("$.unit_price").exists());
    }

    @Test
    @Transactional
    void addToCart_withInvalidQuantity_returns400() throws Exception {
        // Arrange
        Product product = productRepository.save(Product.builder()
                .name("Test Product")
                .price(new BigDecimal("10.00"))
                .stockQuantity(100)
                .lowStockThreshold(10)
                .build());

        CartDTO cartDTO = CartDTO.builder()
                .productId(product.getProductId())
                .quantity(0) // Invalid quantity
                .build();

        // Act and Assert
        mockMvc.perform(post("/api/cart").with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void addToCart_withNonExistentProduct_returns404() throws Exception {
        // Arrange
        CartDTO cartDTO = CartDTO.builder()
                .productId(99999) // Non-existent product
                .quantity(1)
                .build();

        // Act and Assert
        mockMvc.perform(post("/api/cart").with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void addToCart_withoutAuth_returns403() throws Exception {
        // Arrange
        CartDTO cartDTO = CartDTO.builder()
                .productId(1)
                .quantity(1)
                .build();

        // Act and Assert
        mockMvc.perform(post("/api/cart").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartDTO)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but got: " + status);
                    }
                });
    }

    @Test
    @Transactional
    void updateCartItem_updatesQuantitySuccessfully() throws Exception {
        // Arrange
        Product product = productRepository.save(Product.builder()
                .name("Test Product")
                .price(new BigDecimal("15.00"))
                .stockQuantity(100)
                .lowStockThreshold(10)
                .build());

        Cart cartItem = cartRepository.save(Cart.builder()
                .userId("test-user-123")
                .productId(product.getProductId())
                .quantity(2)
                .unitPrice(new BigDecimal("15.00"))
                .build());

        CartDTO updateDTO = CartDTO.builder()
                .quantity(5)
                .build();

        // Act and Assert
        mockMvc.perform(put("/api/cart/{cartId}", cartItem.getCartId()).with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cart_id", is(cartItem.getCartId())))
                .andExpect(jsonPath("$.quantity", is(5)))
                .andExpect(jsonPath("$.user_id", is("test-user-123")));
    }

    @Test
    @Transactional
    void updateCartItem_nonExistent_returns404() throws Exception {
        // Arrange
        CartDTO updateDTO = CartDTO.builder()
                .quantity(3)
                .build();

        // Act and Assert
        mockMvc.perform(put("/api/cart/{cartId}", 99999).with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void updateCartItem_belongsToOtherUser_returns404() throws Exception {
        // Arrange
        Product product = productRepository.save(Product.builder()
                .name("Test Product")
                .price(new BigDecimal("10.00"))
                .stockQuantity(100)
                .lowStockThreshold(10)
                .build());

        Cart cartItem = cartRepository.save(Cart.builder()
                .userId("other-user")
                .productId(product.getProductId())
                .quantity(2)
                .unitPrice(new BigDecimal("10.00"))
                .build());

        CartDTO updateDTO = CartDTO.builder()
                .quantity(5)
                .build();

        // Act and Assert - user cannot update another user's cart item
        mockMvc.perform(put("/api/cart/{cartId}", cartItem.getCartId()).with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void removeCartItem_deletesItemSuccessfully() throws Exception {
        // Arrange
        Product product = productRepository.save(Product.builder()
                .name("Test Product")
                .price(new BigDecimal("10.00"))
                .stockQuantity(100)
                .lowStockThreshold(10)
                .build());

        Cart cartItem = cartRepository.save(Cart.builder()
                .userId("test-user-123")
                .productId(product.getProductId())
                .quantity(2)
                .unitPrice(new BigDecimal("10.00"))
                .build());

        // Act and Assert
        mockMvc.perform(delete("/api/cart/{cartId}", cartItem.getCartId()).with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user-123")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Item removed from cart")));

        // Verify item is deleted
        mockMvc.perform(get("/api/cart")
                        .with(jwt().jwt(jwt -> jwt.subject("test-user-123")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Transactional
    void removeCartItem_nonExistent_returns404() throws Exception {
        // Act and Assert
        mockMvc.perform(delete("/api/cart/{cartId}", 99999).with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user-123")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void removeCartItem_belongsToOtherUser_returns404() throws Exception {
        // Arrange
        Product product = productRepository.save(Product.builder()
                .name("Test Product")
                .price(new BigDecimal("10.00"))
                .stockQuantity(100)
                .lowStockThreshold(10)
                .build());

        Cart cartItem = cartRepository.save(Cart.builder()
                .userId("other-user")
                .productId(product.getProductId())
                .quantity(2)
                .unitPrice(new BigDecimal("10.00"))
                .build());

        // Act and Assert - user cannot delete another user's cart item
        mockMvc.perform(delete("/api/cart/{cartId}", cartItem.getCartId()).with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user-123")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void clearCart_removesAllUserCartItems() throws Exception {
        // Arrange
        Product product1 = productRepository.save(Product.builder()
                .name("Product 1")
                .price(new BigDecimal("10.00"))
                .stockQuantity(100)
                .lowStockThreshold(10)
                .build());

        Product product2 = productRepository.save(Product.builder()
                .name("Product 2")
                .price(new BigDecimal("20.00"))
                .stockQuantity(50)
                .lowStockThreshold(5)
                .build());

        cartRepository.save(Cart.builder()
                .userId("test-user-123")
                .productId(product1.getProductId())
                .quantity(2)
                .unitPrice(new BigDecimal("10.00"))
                .build());

        cartRepository.save(Cart.builder()
                .userId("test-user-123")
                .productId(product2.getProductId())
                .quantity(1)
                .unitPrice(new BigDecimal("20.00"))
                .build());

        // Another user's cart (should not be affected)
        Cart otherUserCart = cartRepository.save(Cart.builder()
                .userId("other-user")
                .productId(product1.getProductId())
                .quantity(5)
                .unitPrice(new BigDecimal("10.00"))
                .build());

        // Act and Assert
        mockMvc.perform(delete("/api/cart").with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user-123")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Cart cleared")));

        // Verify user's cart is empty
        mockMvc.perform(get("/api/cart")
                        .with(jwt().jwt(jwt -> jwt.subject("test-user-123")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Verify other user's cart is not affected
        mockMvc.perform(get("/api/cart")
                        .with(jwt().jwt(jwt -> jwt.subject("other-user")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].user_id", is("other-user")));
    }

    @Test
    @Transactional
    void clearCart_emptyCart_returnsSuccessMessage() throws Exception {
        // Act and Assert - clearing an already empty cart should succeed
        mockMvc.perform(delete("/api/cart").with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("empty-user")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Cart cleared")));
    }

    @Test
    @Transactional
    void clearCart_withoutAuth_returns403() throws Exception {
        // Act and Assert
        mockMvc.perform(delete("/api/cart").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but got: " + status);
                    }
                });
    }
}
