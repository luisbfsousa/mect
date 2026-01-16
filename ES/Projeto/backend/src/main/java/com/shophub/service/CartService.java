package com.shophub.service;

import com.shophub.dto.CartDTO;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.model.Cart;
import com.shophub.model.Product;
import com.shophub.model.User;
import com.shophub.repository.CartRepository;
import com.shophub.repository.ProductRepository;
import com.shophub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    
    @Transactional(readOnly = true)
    public List<Cart> getUserCart(String userId) {
        List<Cart> cartItems = cartRepository.findByUserId(userId);
        
        // Populate product details for each cart item
        cartItems.forEach(cart -> {
            productRepository.findById(cart.getProductId()).ifPresent(product -> {
                cart.setProductName(product.getName());
                cart.setImages(product.getImages());
                // Update unit price to current product price
                cart.setUnitPrice(product.getPrice());
            });
        });
        
        return cartItems;
    }
    
    @Transactional
    public Cart addToCart(String userId, CartDTO cartDTO, Jwt jwt) {
        // Auto-create user if doesn't exist
        userRepository.findById(userId).orElseGet(() -> {
            String email = jwt.getClaimAsString("email");
            String givenName = jwt.getClaimAsString("given_name");
            String familyName = jwt.getClaimAsString("family_name");
            String name = jwt.getClaimAsString("name");
            
            String firstName = givenName != null ? givenName : (name != null ? name.split(" ")[0] : "Customer");
            String lastName = familyName != null ? familyName : (name != null && name.contains(" ") ? name.substring(name.indexOf(" ") + 1) : "");
            
            User newUser = User.builder()
                    .userId(userId)
                    .email(email != null ? email : "no-email@example.com")
                    .firstName(firstName)
                    .lastName(lastName)
                    .role("customer")
                    .build();
            
            User saved = userRepository.save(newUser);
            log.info("Auto-created user from Keycloak: {} ({})", email, userId);
            return saved;
        });
        
        // Get product and validate stock
        Product product = productRepository.findById(cartDTO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        
        if (product.getStockQuantity() < cartDTO.getQuantity()) {
            throw new IllegalArgumentException("Insufficient stock");
        }
        
        // Check if item already exists in cart
        return cartRepository.findByUserIdAndProductId(userId, cartDTO.getProductId())
                .map(existingCart -> {
                    int newQuantity = existingCart.getQuantity() + cartDTO.getQuantity();
                    
                    // Validate total quantity against stock
                    if (product.getStockQuantity() < newQuantity) {
                        throw new IllegalArgumentException("Insufficient stock");
                    }
                    
                    existingCart.setQuantity(newQuantity);
                    existingCart.setUnitPrice(product.getPrice());
                    log.info("Updating cart item quantity for user: {}", userId);
                    
                    Cart saved = cartRepository.save(existingCart);
                    
                    // Populate product details
                    saved.setProductName(product.getName());
                    saved.setImages(product.getImages());
                    
                    return saved;
                })
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .userId(userId)
                            .productId(cartDTO.getProductId())
                            .quantity(cartDTO.getQuantity())
                            .unitPrice(product.getPrice())
                            .build();
                    
                    log.info("Adding new item to cart for user: {}", userId);
                    Cart saved = cartRepository.save(newCart);
                    
                    // Populate product details
                    saved.setProductName(product.getName());
                    saved.setImages(product.getImages());
                    
                    return saved;
                });
    }
    
    @Transactional
    public Cart updateCartItem(String userId, Integer cartId, CartDTO cartDTO) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        
        if (!cart.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to cart item");
        }
        
        if (cartDTO.getQuantity() < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        
        // Validate stock
        Product product = productRepository.findById(cart.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        
        if (product.getStockQuantity() < cartDTO.getQuantity()) {
            throw new IllegalArgumentException("Insufficient stock");
        }
        
        cart.setQuantity(cartDTO.getQuantity());
        cart.setUnitPrice(product.getPrice());
        log.info("Updating cart item: {} for user: {}", cartId, userId);
        
        Cart saved = cartRepository.save(cart);
        
        // Populate product details
        saved.setProductName(product.getName());
        saved.setImages(product.getImages());
        
        return saved;
    }
    
    @Transactional
    public void removeCartItem(String userId, Integer cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        
        if (!cart.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to cart item");
        }
        
        log.info("Removing cart item: {} for user: {}", cartId, userId);
        cartRepository.delete(cart);
    }
    
    @Transactional
    public void clearCart(String userId) {
        log.info("Clearing cart for user: {}", userId);
        cartRepository.deleteByUserId(userId);
    }
}