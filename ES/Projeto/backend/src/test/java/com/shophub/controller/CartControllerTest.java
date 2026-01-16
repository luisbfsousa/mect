package com.shophub.controller;

import com.shophub.dto.CartDTO;
import com.shophub.model.Cart;
import com.shophub.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private CartController cartController;

    private Cart sampleCartItem;
    private CartDTO addToCartRequest;
    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = "user-123";
        
        // Mock JWT
        when(jwt.getSubject()).thenReturn(testUserId);
        
        // Sample cart item
        sampleCartItem = Cart.builder()
                .cartId(1)
                .userId(testUserId)
                .productId(101)
                .quantity(2)
                .unitPrice(new BigDecimal("29.99"))
                .productName("Wireless Keyboard")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        // Add to cart request DTO
        addToCartRequest = CartDTO.builder()
                .productId(101)
                .quantity(2)
                .build();
    }

    /**
     * Test: Successfully add a new item to the shopping cart
     */
    @Test
    void addToCart_ShouldAddNewItem_WhenProductNotInCart() {
        // Given
        when(cartService.addToCart(eq(testUserId), any(CartDTO.class), eq(jwt)))
                .thenReturn(sampleCartItem);

        // When
        ResponseEntity<Cart> response = cartController.addToCart(jwt, addToCartRequest);

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        
        Cart cartItem = response.getBody();
        assertEquals(1, cartItem.getCartId());
        assertEquals(testUserId, cartItem.getUserId());
        assertEquals(101, cartItem.getProductId());
        assertEquals(2, cartItem.getQuantity());
        assertEquals(new BigDecimal("29.99"), cartItem.getUnitPrice());
        assertEquals("Wireless Keyboard", cartItem.getProductName());
        
        verify(cartService, times(1)).addToCart(eq(testUserId), any(CartDTO.class), eq(jwt));
    }

    /**
     * Test: Get user's cart after adding items
     */
    @Test
    void getUserCart_ShouldReturnAllCartItems_AfterAddingItems() {
        // Given
        Cart item1 = Cart.builder()
                .cartId(1)
                .userId(testUserId)
                .productId(101)
                .quantity(2)
                .unitPrice(new BigDecimal("29.99"))
                .productName("Wireless Keyboard")
                .build();
        
        Cart item2 = Cart.builder()
                .cartId(2)
                .userId(testUserId)
                .productId(102)
                .quantity(1)
                .unitPrice(new BigDecimal("49.99"))
                .productName("Gaming Mouse")
                .build();
        
        List<Cart> cartItems = Arrays.asList(item1, item2);
        when(cartService.getUserCart(testUserId)).thenReturn(cartItems);

        // When
        ResponseEntity<List<Cart>> response = cartController.getUserCart(jwt);

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size(), "Cart should have 2 items");
        
        // Verify first item
        assertEquals("Wireless Keyboard", response.getBody().get(0).getProductName());
        assertEquals(2, response.getBody().get(0).getQuantity());
        
        // Verify second item
        assertEquals("Gaming Mouse", response.getBody().get(1).getProductName());
        assertEquals(1, response.getBody().get(1).getQuantity());
        
        verify(cartService, times(1)).getUserCart(testUserId);
    }

    /**
     * Test: Get empty cart
     */
    @Test
    void getUserCart_ShouldReturnEmptyList_WhenCartIsEmpty() {
        // Given
        when(cartService.getUserCart(testUserId)).thenReturn(Arrays.asList());

        // When
        ResponseEntity<List<Cart>> response = cartController.getUserCart(jwt);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size(), "Cart should be empty");
        
        verify(cartService, times(1)).getUserCart(testUserId);
    }

    /**
     * Test: Verify correct user ID is extracted from JWT
     */
    @Test
    void addToCart_ShouldUseAuthenticatedUserId_FromJWT() {
        // Given
        when(cartService.addToCart(eq(testUserId), any(CartDTO.class), eq(jwt)))
                .thenReturn(sampleCartItem);

        // When
        cartController.addToCart(jwt, addToCartRequest);

        // Then
        verify(jwt, atLeastOnce()).getSubject();
        verify(cartService).addToCart(eq(testUserId), any(CartDTO.class), eq(jwt));
    }

    /**
     * Test: Verify cart item contains all essential information
     */
    @Test
    void addToCart_ShouldReturnCompleteCartInformation() {
        // Given
        when(cartService.addToCart(eq(testUserId), any(CartDTO.class), eq(jwt)))
                .thenReturn(sampleCartItem);

        // When
        ResponseEntity<Cart> response = cartController.addToCart(jwt, addToCartRequest);

        // Then
        Cart cartItem = response.getBody();
        assertNotNull(cartItem, "Cart item should not be null");
        assertNotNull(cartItem.getCartId(), "Cart ID should not be null");
        assertNotNull(cartItem.getUserId(), "User ID should not be null");
        assertNotNull(cartItem.getProductId(), "Product ID should not be null");
        assertNotNull(cartItem.getQuantity(), "Quantity should not be null");
        assertNotNull(cartItem.getUnitPrice(), "Unit price should not be null");
        assertNotNull(cartItem.getProductName(), "Product name should not be null");
        
        assertTrue(cartItem.getQuantity() > 0, "Quantity should be positive");
        assertTrue(cartItem.getUnitPrice().compareTo(BigDecimal.ZERO) > 0, 
                "Unit price should be positive");
    }

    /**
     * Test: Update existing cart item quantity
     */
    @Test
    void updateCartItem_ShouldUpdateQuantity_WhenItemExists() {
        // Given
        Integer cartId = 1;
        CartDTO updateRequest = CartDTO.builder()
                .quantity(5)
                .build();
        
        Cart updatedCart = Cart.builder()
                .cartId(cartId)
                .userId(testUserId)
                .productId(101)
                .quantity(5)
                .unitPrice(new BigDecimal("29.99"))
                .productName("Wireless Keyboard")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(cartService.updateCartItem(eq(testUserId), eq(cartId), any(CartDTO.class)))
                .thenReturn(updatedCart);

        // When
        ResponseEntity<Cart> response = cartController.updateCartItem(jwt, cartId, updateRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5, response.getBody().getQuantity(), "Quantity should be updated to 5");
        
        verify(cartService, times(1)).updateCartItem(eq(testUserId), eq(cartId), any(CartDTO.class));
    }
}
