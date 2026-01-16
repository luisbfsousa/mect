package com.shophub.controller;

import com.shophub.dto.CartDTO;
import com.shophub.model.Cart;
import com.shophub.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
// @CrossOrigin(origins = "*") 
public class CartController {
    
    private final CartService cartService;
    
    @GetMapping
    public ResponseEntity<List<Cart>> getUserCart(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(cartService.getUserCart(userId));
    }
    
    @PostMapping
    public ResponseEntity<Cart> addToCart(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CartDTO cartDTO) {
        String userId = jwt.getSubject();
        Cart cart = cartService.addToCart(userId, cartDTO, jwt);
        return ResponseEntity.ok(cart);
    }
    
    @PutMapping("/{cartId}")
    public ResponseEntity<Cart> updateCartItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer cartId,
            @Valid @RequestBody CartDTO cartDTO) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(cartService.updateCartItem(userId, cartId, cartDTO));
    }
    
    @DeleteMapping("/{cartId}")
    public ResponseEntity<Map<String, String>> removeCartItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer cartId) {
        String userId = jwt.getSubject();
        cartService.removeCartItem(userId, cartId);
        return ResponseEntity.ok(Map.of("message", "Item removed from cart"));
    }
    
    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearCart(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        cartService.clearCart(userId);
        return ResponseEntity.ok(Map.of("message", "Cart cleared"));
    }
}
