package com.shophub.service;

import com.shophub.dto.CartDTO;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.model.Cart;
import com.shophub.model.Product;
import com.shophub.model.User;
import com.shophub.repository.CartRepository;
import com.shophub.repository.ProductRepository;
import com.shophub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private CartService cartService;

    private Cart testCart;
    private Product testProduct;
    private User testUser;
    private CartDTO testCartDTO;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .productId(1)
                .name("Test Product")
                .price(new BigDecimal("99.99"))
                .stockQuantity(10)
                .images(Arrays.asList("image1.jpg"))
                .build();

        testUser = User.builder()
                .userId("user123")
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .role("customer")
                .build();

        testCart = Cart.builder()
                .cartId(1)
                .userId("user123")
                .productId(1)
                .quantity(2)
                .unitPrice(new BigDecimal("99.99"))
                .build();

        testCartDTO = CartDTO.builder()
                .productId(1)
                .quantity(2)
                .build();
    }

    @Test
    void getUserCart_ShouldReturnCartItemsWithProductDetails() {
        // Given
        List<Cart> cartItems = Arrays.asList(testCart);
        when(cartRepository.findByUserId("user123")).thenReturn(cartItems);
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));

        // When
        List<Cart> result = cartService.getUserCart("user123");

        // Then
        assertEquals(1, result.size());
        assertEquals("Test Product", result.get(0).getProductName());
        assertEquals(new BigDecimal("99.99"), result.get(0).getUnitPrice());
        verify(cartRepository).findByUserId("user123");
        verify(productRepository).findById(1);
    }

    @Test
    void addToCart_ShouldCreateNewCartItemWhenNotExists() {
        // Given
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
        when(cartRepository.findByUserIdAndProductId("user123", 1)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // When
        Cart result = cartService.addToCart("user123", testCartDTO, jwt);

        // Then
        assertNotNull(result);
        verify(userRepository).findById("user123");
        verify(productRepository).findById(1);
        verify(cartRepository).findByUserIdAndProductId("user123", 1);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addToCart_ShouldUpdateExistingCartItem() {
        // Given
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
        when(cartRepository.findByUserIdAndProductId("user123", 1)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // When
        Cart result = cartService.addToCart("user123", testCartDTO, jwt);

        // Then
        assertNotNull(result);
        assertEquals(4, testCart.getQuantity()); // 2 + 2
        verify(cartRepository).save(testCart);
    }

    @Test
    void addToCart_ShouldAutoCreateUserWhenNotExists() {
        // Given
        when(userRepository.findById("user123")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
        when(jwt.getClaimAsString("given_name")).thenReturn("John");
        when(jwt.getClaimAsString("family_name")).thenReturn("Doe");
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
        when(cartRepository.findByUserIdAndProductId("user123", 1)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // When
        Cart result = cartService.addToCart("user123", testCartDTO, jwt);

        // Then
        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void addToCart_ShouldThrowExceptionWhenProductNotFound() {
        // Given
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> cartService.addToCart("user123", testCartDTO, jwt));
        verify(productRepository).findById(1);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void addToCart_ShouldThrowExceptionWhenInsufficientStock() {
        // Given
        testProduct.setStockQuantity(1);
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> cartService.addToCart("user123", testCartDTO, jwt));
        verify(productRepository).findById(1);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void updateCartItem_ShouldUpdateCartItemSuccessfully() {
        // Given
        when(cartRepository.findById(1)).thenReturn(Optional.of(testCart));
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // When
        Cart result = cartService.updateCartItem("user123", 1, testCartDTO);

        // Then
        assertEquals(2, result.getQuantity());
        verify(cartRepository).findById(1);
        verify(productRepository).findById(1);
        verify(cartRepository).save(testCart);
    }

    @Test
    void updateCartItem_ShouldThrowExceptionWhenCartNotFound() {
        // Given
        when(cartRepository.findById(1)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> cartService.updateCartItem("user123", 1, testCartDTO));
        verify(cartRepository).findById(1);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void updateCartItem_ShouldThrowExceptionWhenUnauthorized() {
        // Given
        testCart.setUserId("otheruser");
        when(cartRepository.findById(1)).thenReturn(Optional.of(testCart));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> cartService.updateCartItem("user123", 1, testCartDTO));
        verify(cartRepository).findById(1);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void updateCartItem_ShouldThrowExceptionWhenInsufficientStock() {
        // Given
        testProduct.setStockQuantity(1);
        when(cartRepository.findById(1)).thenReturn(Optional.of(testCart));
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> cartService.updateCartItem("user123", 1, testCartDTO));
        verify(cartRepository).findById(1);
        verify(productRepository).findById(1);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void removeCartItem_ShouldRemoveCartItemSuccessfully() {
        // Given
        when(cartRepository.findById(1)).thenReturn(Optional.of(testCart));

        // When
        cartService.removeCartItem("user123", 1);

        // Then
        verify(cartRepository).findById(1);
        verify(cartRepository).delete(testCart);
    }

    @Test
    void removeCartItem_ShouldThrowExceptionWhenUnauthorized() {
        // Given
        testCart.setUserId("otheruser");
        when(cartRepository.findById(1)).thenReturn(Optional.of(testCart));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> cartService.removeCartItem("user123", 1));
        verify(cartRepository).findById(1);
        verify(cartRepository, never()).delete(any(Cart.class));
    }

    @Test
    void clearCart_ShouldClearUserCart() {
        // When
        cartService.clearCart("user123");

        // Then
        verify(cartRepository).deleteByUserId("user123");
    }
}
