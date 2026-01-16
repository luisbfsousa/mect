package com.shophub.controller;

import com.shophub.dto.*;
import com.shophub.model.Order;
import com.shophub.model.Product;
import com.shophub.model.User;
import com.shophub.model.ShippingBillingInfo;
import com.shophub.service.OrderService;
import com.shophub.service.ProductService;
import com.shophub.service.CustomerAdminService;
import com.shophub.service.AuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('administrator')")
public class AdminController {
    
    private final OrderService orderService;
    private final ProductService productService;
    private final CustomerAdminService customerAdminService;
    private final AuditLogService auditLogService;
    
    // ========== CUSTOMER MANAGEMENT ENDPOINTS ==========
    
    @GetMapping("/customers")
    public ResponseEntity<List<User>> getAllCustomers() {
        return ResponseEntity.ok(customerAdminService.getAllCustomers());
    }
    
    @GetMapping("/customers/{customerId}")
    public ResponseEntity<User> getCustomerDetails(@PathVariable String customerId) {
        return ResponseEntity.ok(customerAdminService.getCustomerDetails(customerId));
    }
    
    @GetMapping("/customers/{customerId}/audit-logs")
    public ResponseEntity<List<AuditLogDTO>> getCustomerAuditLogs(@PathVariable String customerId) {
        return ResponseEntity.ok(auditLogService.getAuditLogsByTargetUser(customerId));
    }
    
    /**
     * Admin updates customer personal details (name, email, phone)
     */
    @PutMapping("/customers/{customerId}/details")
    public ResponseEntity<User> updateCustomerDetails(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String customerId,
            @Valid @RequestBody UpdateCustomerDetailsRequest request) {
        String adminId = jwt.getSubject();
        User updatedCustomer = customerAdminService.updateCustomerDetails(adminId, customerId, request);
        return ResponseEntity.ok(updatedCustomer);
    }
    
    /**
     * Admin updates customer shipping and billing addresses
     */
    @PutMapping("/customers/{customerId}/addresses")
    public ResponseEntity<ShippingBillingInfo> updateCustomerAddresses(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String customerId,
            @Valid @RequestBody UpdateCustomerAddressesRequest request) {
        String adminId = jwt.getSubject();
        ShippingBillingInfo updatedAddresses = customerAdminService.updateCustomerAddresses(adminId, customerId, request);
        return ResponseEntity.ok(updatedAddresses);
    }
    
    /**
     * Admin resets customer password
     */
    @PostMapping("/customers/{customerId}/reset-password")
    public ResponseEntity<Map<String, String>> resetCustomerPassword(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String customerId,
            @Valid @RequestBody ResetCustomerPasswordRequest request) {
        String adminId = jwt.getSubject();
        customerAdminService.resetCustomerPassword(adminId, customerId, request);
        return ResponseEntity.ok(Map.of(
                "message", "Password reset initiated. User will be notified.",
                "customerId", customerId
        ));
    }
    
    /**
     * Admin locks a customer account
     */
    @PostMapping("/customers/{customerId}/lock")
    public ResponseEntity<User> lockCustomerAccount(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String customerId,
            @RequestParam(required = false, defaultValue = "Security violation") String reason) {
        String adminId = jwt.getSubject();
        User lockedCustomer = customerAdminService.lockAccount(adminId, customerId, reason);
        return ResponseEntity.ok(lockedCustomer);
    }
    
    /**
     * Admin unlocks a customer account
     */
    @PostMapping("/customers/{customerId}/unlock")
    public ResponseEntity<User> unlockCustomerAccount(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String customerId,
            @RequestParam(required = false, defaultValue = "Account unlocked") String reason) {
        String adminId = jwt.getSubject();
        User unlockedCustomer = customerAdminService.unlockAccount(adminId, customerId, reason);
        return ResponseEntity.ok(unlockedCustomer);
    }
    
    /**
     * Admin deactivates a customer account
     */
    @PostMapping("/customers/{customerId}/deactivate")
    public ResponseEntity<User> deactivateCustomerAccount(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String customerId,
            @RequestParam(required = false, defaultValue = "Account deactivated by administrator") String reason) {
        String adminId = jwt.getSubject();
        User deactivatedCustomer = customerAdminService.deactivateAccount(adminId, customerId, reason);
        return ResponseEntity.ok(deactivatedCustomer);
    }
    
    /**
     * Admin reactivates a customer account
     */
    @PostMapping("/customers/{customerId}/reactivate")
    public ResponseEntity<User> reactivateCustomerAccount(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String customerId,
            @RequestParam(required = false, defaultValue = "Account reactivated") String reason) {
        String adminId = jwt.getSubject();
        User reactivatedCustomer = customerAdminService.reactivateAccount(adminId, customerId, reason);
        return ResponseEntity.ok(reactivatedCustomer);
    }
    
    // ========== ORDER ENDPOINTS ==========
    
    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(orderService.getAllOrders(status, search));
    }
    
    @GetMapping("/orders/stats")
    public ResponseEntity<Map<String, Object>> getOrderStatistics() {
        // TODO: Implement statistics
        return ResponseEntity.ok(Map.of(
            "message", "Order statistics endpoint",
            "status", "not_implemented"
        ));
    }
    
    /**
     * Admin confirms payment for a pending order
     */
    @PostMapping("/orders/{id}/confirm-payment")
    public ResponseEntity<Order> confirmPayment(@PathVariable Integer id) {
        Order order = orderService.confirmPayment(id);
        return ResponseEntity.ok(order);
    }
    
    /**
     * Admin updates order with comprehensive details
     */
    @PutMapping("/orders/{id}")
    public ResponseEntity<Order> updateOrder(
            @PathVariable Integer id,
            @Valid @RequestBody AdminOrderUpdateRequest request) {
        Order order = orderService.adminUpdateOrder(id, request);
        return ResponseEntity.ok(order);
    }
    
    /**
     * Admin marks order as shipped with tracking information
     */
    @PostMapping("/orders/{id}/ship")
    public ResponseEntity<Order> markAsShipped(
            @PathVariable Integer id,
            @RequestParam(required = false) String trackingNumber,
            @RequestParam(required = false) String shippingProvider) {
        Order order = orderService.markAsShipped(id, trackingNumber, shippingProvider);
        return ResponseEntity.ok(order);
    }
    
    // ========== PRODUCT ENDPOINTS ==========
    
    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }
    
    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }
    
    @PostMapping("/products")
    public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductDTO productDTO) {
        Product product = productService.createProduct(productDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }
    
    @PutMapping("/products/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Integer id,
            @Valid @RequestBody ProductDTO productDTO) {
        return ResponseEntity.ok(productService.updateProduct(id, productDTO));
    }
    
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable Integer id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
    }
    
    @PutMapping("/products/{id}/inventory")
    public ResponseEntity<Product> updateInventory(
            @PathVariable Integer id,
            @Valid @RequestBody InventoryUpdateRequest request) {
        Product product = productService.setStockQuantity(id, request.getStockQuantity());
        return ResponseEntity.ok(product);
    }
}