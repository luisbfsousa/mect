package com.shophub.service;

import com.shophub.dto.CartDTO;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.model.Cart;
import com.shophub.model.Product;
import com.shophub.repository.CartRepository;
import com.shophub.repository.ProductRepository;
import com.shophub.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
public class CartServiceIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanup() {
        cartRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Jwt createTestJwt(String userId) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", userId)
                .claim("email", "test@example.com")
                .claim("given_name", "Test")
                .claim("family_name", "User")
                .claim("name", "Test User")
                .build();
    }

    @Test
    @Transactional
    void addToCart_createsNewCartItem() {
        // Arrange
        Product product = Product.builder()
                .name("Test Product")
                .description("Test description")
                .price(new BigDecimal("10.00"))
                .stockQuantity(50)
                .lowStockThreshold(5)
                .build();
        product = productRepository.save(product);

        CartDTO cartDTO = CartDTO.builder()
                .productId(product.getProductId())
                .quantity(2)
                .build();

        String userId = "test-user-1";
        Jwt jwt = createTestJwt(userId);

        // Act
        Cart added = cartService.addToCart(userId, cartDTO, jwt);

        // Assert
        assertThat(added).isNotNull();
        assertThat(added.getCartId()).isNotNull();
        assertThat(added.getUserId()).isEqualTo(userId);
        assertThat(added.getProductId()).isEqualTo(product.getProductId());
        assertThat(added.getQuantity()).isEqualTo(2);
        assertThat(added.getUnitPrice()).isEqualByComparingTo(new BigDecimal("10.00"));

        // Verify user was auto-created
        assertThat(userRepository.existsById(userId)).isTrue();

        // Verify in database
        Cart fromDb = cartRepository.findById(added.getCartId()).orElseThrow();
        assertThat(fromDb.getQuantity()).isEqualTo(2);
    }

    @Test
    @Transactional
    void addToCart_updatesExistingCartItem() {
        // Arrange - create product and existing cart item
        Product product = Product.builder()
                .name("Test Product")
                .description("Test description")
                .price(new BigDecimal("15.00"))
                .stockQuantity(50)
                .lowStockThreshold(5)
                .build();
        product = productRepository.save(product);

        String userId = "test-user-2";
        Jwt jwt = createTestJwt(userId);

        // Add item first time
        CartDTO firstAdd = CartDTO.builder()
                .productId(product.getProductId())
                .quantity(3)
                .build();
        Cart firstCart = cartService.addToCart(userId, firstAdd, jwt);

        // Act - add same product again
        CartDTO secondAdd = CartDTO.builder()
                .productId(product.getProductId())
                .quantity(2)
                .build();
        Cart updated = cartService.addToCart(userId, secondAdd, jwt);

        // Assert
        assertThat(updated.getCartId()).isEqualTo(firstCart.getCartId());
        assertThat(updated.getQuantity()).isEqualTo(5);
        assertThat(updated.getUnitPrice()).isEqualByComparingTo(new BigDecimal("15.00"));

        // Verify only one cart item exists
        List<Cart> allItems = cartRepository.findByUserId(userId);
        assertThat(allItems).hasSize(1);
    }

    @Test
    @Transactional
    void addToCart_throwsExceptionWhenInsufficientStock() {
        // Arrange
        Product product = Product.builder()
                .name("Low Stock Product")
                .description("Only 5 in stock")
                .price(new BigDecimal("20.00"))
                .stockQuantity(5)
                .lowStockThreshold(1)
                .build();
        product = productRepository.save(product);

        CartDTO cartDTO = CartDTO.builder()
                .productId(product.getProductId())
                .quantity(10)
                .build();

        String userId = "test-user-3";
        Jwt jwt = createTestJwt(userId);

        // Act & Assert
        assertThatThrownBy(() -> cartService.addToCart(userId, cartDTO, jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @Transactional
    void getUserCart_returnsAllItemsForUser() {
        // Arrange - create products and cart items
        Product product1 = productRepository.save(Product.builder()
                .name("Product 1")
                .description("Description 1")
                .price(new BigDecimal("10.00"))
                .stockQuantity(100)
                .lowStockThreshold(5)
                .build());

        Product product2 = productRepository.save(Product.builder()
                .name("Product 2")
                .description("Description 2")
                .price(new BigDecimal("20.00"))
                .stockQuantity(100)
                .lowStockThreshold(5)
                .build());

        String userId = "test-user-4";
        Jwt jwt = createTestJwt(userId);

        cartService.addToCart(userId, CartDTO.builder()
                .productId(product1.getProductId())
                .quantity(2)
                .build(), jwt);

        cartService.addToCart(userId, CartDTO.builder()
                .productId(product2.getProductId())
                .quantity(3)
                .build(), jwt);

        // Act
        List<Cart> userCart = cartService.getUserCart(userId);

        // Assert
        assertThat(userCart).hasSize(2);
        assertThat(userCart).extracting("productId")
                .containsExactlyInAnyOrder(product1.getProductId(), product2.getProductId());
        assertThat(userCart).extracting("productName")
                .containsExactlyInAnyOrder("Product 1", "Product 2");
    }

    @Test
    @Transactional
    void updateCartItem_updatesQuantity() {
        // Arrange - create product and cart item
        Product product = Product.builder()
                .name("Test Product")
                .description("Test description")
                .price(new BigDecimal("25.00"))
                .stockQuantity(50)
                .lowStockThreshold(5)
                .build();
        product = productRepository.save(product);

        String userId = "test-user-5";
        Jwt jwt = createTestJwt(userId);

        Cart initialCart = cartService.addToCart(userId, CartDTO.builder()
                .productId(product.getProductId())
                .quantity(5)
                .build(), jwt);

        // Act - update quantity
        CartDTO updateDTO = CartDTO.builder()
                .quantity(10)
                .build();
        Cart updated = cartService.updateCartItem(userId, initialCart.getCartId(), updateDTO);

        // Assert
        assertThat(updated.getCartId()).isEqualTo(initialCart.getCartId());
        assertThat(updated.getQuantity()).isEqualTo(10);
        assertThat(updated.getUnitPrice()).isEqualByComparingTo(new BigDecimal("25.00"));

        // Verify in database
        Cart fromDb = cartRepository.findById(initialCart.getCartId()).orElseThrow();
        assertThat(fromDb.getQuantity()).isEqualTo(10);
    }

    @Test
    @Transactional
    void updateCartItem_throwsExceptionForUnauthorizedUser() {
        // Arrange
        Product product = productRepository.save(Product.builder()
                .name("Test Product")
                .description("Test description")
                .price(new BigDecimal("10.00"))
                .stockQuantity(50)
                .lowStockThreshold(5)
                .build());

        String user1 = "user-1";
        Cart cart = cartService.addToCart(user1, CartDTO.builder()
                .productId(product.getProductId())
                .quantity(2)
                .build(), createTestJwt(user1));

        // Act & Assert - different user tries to update
        String user2 = "user-2";
        CartDTO updateDTO = CartDTO.builder().quantity(5).build();

        assertThatThrownBy(() -> cartService.updateCartItem(user2, cart.getCartId(), updateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unauthorized access");
    }

    @Test
    @Transactional
    void removeCartItem_deletesItem() {
        // Arrange
        Product product = Product.builder()
                .name("Test Product")
                .description("Test description")
                .price(new BigDecimal("15.00"))
                .stockQuantity(50)
                .lowStockThreshold(5)
                .build();
        product = productRepository.save(product);

        String userId = "test-user-6";
        Jwt jwt = createTestJwt(userId);

        Cart cart = cartService.addToCart(userId, CartDTO.builder()
                .productId(product.getProductId())
                .quantity(3)
                .build(), jwt);

        Integer cartId = cart.getCartId();

        // Act
        cartService.removeCartItem(userId, cartId);

        // Assert
        assertThat(cartRepository.findById(cartId)).isEmpty();
        assertThat(cartService.getUserCart(userId)).isEmpty();
    }

    @Test
    @Transactional
    void removeCartItem_throwsExceptionWhenNotFound() {
        // Act & Assert
        assertThatThrownBy(() -> cartService.removeCartItem("user-1", 99999))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cart item not found");
    }

    @Test
    @Transactional
    void clearCart_removesAllItemsForUser() {
        // Arrange - add multiple items
        Product product1 = productRepository.save(Product.builder()
                .name("Product 1")
                .description("Description 1")
                .price(new BigDecimal("10.00"))
                .stockQuantity(100)
                .lowStockThreshold(5)
                .build());

        Product product2 = productRepository.save(Product.builder()
                .name("Product 2")
                .description("Description 2")
                .price(new BigDecimal("20.00"))
                .stockQuantity(100)
                .lowStockThreshold(5)
                .build());

        String userId = "test-user-7";
        Jwt jwt = createTestJwt(userId);

        cartService.addToCart(userId, CartDTO.builder()
                .productId(product1.getProductId())
                .quantity(2)
                .build(), jwt);

        cartService.addToCart(userId, CartDTO.builder()
                .productId(product2.getProductId())
                .quantity(3)
                .build(), jwt);

        // Verify items exist
        assertThat(cartService.getUserCart(userId)).hasSize(2);

        // Act
        cartService.clearCart(userId);

        // Assert
        assertThat(cartService.getUserCart(userId)).isEmpty();
        assertThat(cartRepository.findByUserId(userId)).isEmpty();
    }

    @Test
    @Transactional
    void addToCart_throwsExceptionWhenProductNotFound() {
        // Arrange
        CartDTO cartDTO = CartDTO.builder()
                .productId(99999) // Non-existent product
                .quantity(1)
                .build();

        String userId = "test-user-8";
        Jwt jwt = createTestJwt(userId);

        // Act & Assert
        assertThatThrownBy(() -> cartService.addToCart(userId, cartDTO, jwt))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");
    }
}
