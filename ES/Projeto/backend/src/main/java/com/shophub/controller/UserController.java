package com.shophub.controller;

import com.shophub.dto.ShippingBillingDTO;
import com.shophub.dto.UserDTO;
import com.shophub.model.User;
import com.shophub.service.ShippingBillingService;
import com.shophub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final ShippingBillingService shippingBillingService;
    
    @GetMapping
    public ResponseEntity<User> getProfile(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.getOrCreateUser(jwt);
        return ResponseEntity.ok(user);
    }
    
    @PutMapping
    public ResponseEntity<User> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserDTO userDTO) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(userService.updateProfile(userId, userDTO));
    }
    
    /**
     * Get saved shipping and billing information for the authenticated user
     */
    @GetMapping("/shipping-billing")
    public ResponseEntity<ShippingBillingDTO> getShippingBilling(
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        ShippingBillingDTO info = shippingBillingService.getShippingBillingInfo(userId);
        return ResponseEntity.ok(info);
    }
    
    /**
     * Save or update shipping and billing information for the authenticated user
     */
    @PutMapping("/shipping-billing")
    public ResponseEntity<ShippingBillingDTO> updateShippingBilling(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ShippingBillingDTO shippingBillingDTO) {
        String userId = jwt.getSubject();
        ShippingBillingDTO updated = shippingBillingService.updateShippingBillingInfo(userId, shippingBillingDTO);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * Delete saved shipping and billing information for the authenticated user
     */
    @DeleteMapping("/shipping-billing")
    public ResponseEntity<Map<String, String>> deleteShippingBilling(
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        shippingBillingService.deleteShippingBillingInfo(userId);
        return ResponseEntity.ok(Map.of("message", "Shipping and billing info deleted successfully"));
    }
}