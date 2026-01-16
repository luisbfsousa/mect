package com.shophub.service;

import com.shophub.dto.UpdateCustomerDetailsRequest;
import com.shophub.dto.UpdateCustomerAddressesRequest;
import com.shophub.dto.ResetCustomerPasswordRequest;
import com.shophub.dto.AccountStatusRequest;
import com.shophub.model.User;
import com.shophub.model.ShippingBillingInfo;
import com.shophub.repository.ShippingBillingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerAdminService {
    
    private final UserService userService;
    private final ShippingBillingRepository shippingBillingRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final KeycloakAdminService keycloakAdminService;
    
    @Transactional(readOnly = true)
    public List<User> getAllCustomers() {
        return userService.getAllCustomers();
    }
    
    @Transactional(readOnly = true)
    public User getCustomerDetails(String customerId) {
        return userService.getUserById(customerId);
    }
    
    @Transactional
    public User updateCustomerDetails(String adminId, String customerId, UpdateCustomerDetailsRequest request) {
        User customer = userService.getUserById(customerId);
        
        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("action", "UPDATE_CUSTOMER_DETAILS");
        auditDetails.put("oldFirstName", customer.getFirstName());
        auditDetails.put("oldLastName", customer.getLastName());
        auditDetails.put("oldEmail", customer.getEmail());
        auditDetails.put("oldPhone", customer.getPhone());
        auditDetails.put("newFirstName", request.getFirstName());
        auditDetails.put("newLastName", request.getLastName());
        auditDetails.put("newEmail", request.getEmail());
        auditDetails.put("newPhone", request.getPhone());
        
        User updatedCustomer = userService.updateCustomerDetails(
                customerId,
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPhone()
        );

        // Sync to Keycloak (best-effort)
        try {
            keycloakAdminService.updateUserProfile(
                customerId,
                request.getFirstName(),
                request.getLastName(),
                request.getEmail()
            );
        } catch (Exception e) {
            log.warn("Failed to sync user {} updates to Keycloak: {}", customerId, e.getMessage());
        }
        
        auditLogService.logAdminAction(adminId, customerId, "UPDATE_CUSTOMER_DETAILS", auditDetails);
        
        log.info("Admin {} updated customer details for user {}", adminId, customerId);
        
        return updatedCustomer;
    }
    
    @Transactional
    public ShippingBillingInfo updateCustomerAddresses(String adminId, String customerId, UpdateCustomerAddressesRequest request) {
        ShippingBillingInfo addressInfo = shippingBillingRepository.findByUserId(customerId)
                .orElseGet(() -> ShippingBillingInfo.builder()
                        .userId(customerId)
                        .createdAt(LocalDateTime.now())
                        .build());
        
        // Save old values for audit
        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("action", "UPDATE_CUSTOMER_ADDRESSES");
        
        // Shipping info
        if (request.getShippingInfo() != null) {
            auditDetails.put("oldShippingInfo", Map.of(
                    "fullName", addressInfo.getShippingFullName(),
                    "address", addressInfo.getShippingAddress(),
                    "city", addressInfo.getShippingCity(),
                    "postalCode", addressInfo.getShippingPostalCode(),
                    "phone", addressInfo.getShippingPhone()
            ));
            
            addressInfo.setShippingFullName(request.getShippingInfo().getFullName());
            addressInfo.setShippingAddress(request.getShippingInfo().getAddress());
            addressInfo.setShippingCity(request.getShippingInfo().getCity());
            addressInfo.setShippingPostalCode(request.getShippingInfo().getPostalCode());
            addressInfo.setShippingPhone(request.getShippingInfo().getPhone());
            
            auditDetails.put("newShippingInfo", Map.of(
                    "fullName", request.getShippingInfo().getFullName(),
                    "address", request.getShippingInfo().getAddress(),
                    "city", request.getShippingInfo().getCity(),
                    "postalCode", request.getShippingInfo().getPostalCode(),
                    "phone", request.getShippingInfo().getPhone()
            ));
        }
        
        // Billing info
        if (request.getBillingInfo() != null) {
            auditDetails.put("oldBillingInfo", Map.of(
                    "fullName", addressInfo.getBillingFullName(),
                    "address", addressInfo.getBillingAddress(),
                    "city", addressInfo.getBillingCity(),
                    "postalCode", addressInfo.getBillingPostalCode(),
                    "phone", addressInfo.getBillingPhone()
            ));
            
            addressInfo.setBillingFullName(request.getBillingInfo().getFullName());
            addressInfo.setBillingAddress(request.getBillingInfo().getAddress());
            addressInfo.setBillingCity(request.getBillingInfo().getCity());
            addressInfo.setBillingPostalCode(request.getBillingInfo().getPostalCode());
            addressInfo.setBillingPhone(request.getBillingInfo().getPhone());
            
            auditDetails.put("newBillingInfo", Map.of(
                    "fullName", request.getBillingInfo().getFullName(),
                    "address", request.getBillingInfo().getAddress(),
                    "city", request.getBillingInfo().getCity(),
                    "postalCode", request.getBillingInfo().getPostalCode(),
                    "phone", request.getBillingInfo().getPhone()
            ));
        }
        
        addressInfo.setUpdatedAt(LocalDateTime.now());
        ShippingBillingInfo updated = shippingBillingRepository.save(addressInfo);
        
        auditLogService.logAdminAction(adminId, customerId, "UPDATE_CUSTOMER_ADDRESSES", auditDetails);
        
        log.info("Admin {} updated customer addresses for user {}", adminId, customerId);
        
        return updated;
    }
    
    @Transactional
    public void resetCustomerPassword(String adminId, String customerId, ResetCustomerPasswordRequest request) {
        // Password reset is handled via Keycloak integration
        // This logs the admin action and notification
        
        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("action", "RESET_PASSWORD");
        auditDetails.put("sendNotification", request.getSendNotification());
        auditDetails.put("timestamp", LocalDateTime.now());
        
        auditLogService.logAdminAction(adminId, customerId, "RESET_PASSWORD", auditDetails);
        
        if (request.getSendNotification()) {
            try {
                notificationService.sendNotification(
                        customerId,
                        "Password Reset",
                        "Your password has been reset by an administrator. Please log in with your new password.",
                        "admin_action"
                );
            } catch (Exception e) {
                log.warn("Failed to send password reset notification to user {}: {}", customerId, e.getMessage());
            }
        }
        
        log.info("Admin {} reset password for user {}. Notification sent: {}", adminId, customerId, request.getSendNotification());
    }
    
    @Transactional
    public User lockAccount(String adminId, String customerId, String reason) {
        if (adminId != null && adminId.equals(customerId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN,
                "Administrators cannot lock their own account"
            );
        }
        User customer = userService.lockAccount(customerId);
        
        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("action", "LOCK_ACCOUNT");
        auditDetails.put("reason", reason);
        
        auditLogService.logAdminAction(adminId, customerId, "LOCK_ACCOUNT", auditDetails);
        
        try {
                notificationService.sendNotification(
                    customerId,
                    "Account Locked",
                    "Your account has been locked by an administrator.",
                    "admin_action"
                );
        } catch (Exception e) {
            log.warn("Failed to send account locked notification to user {}: {}", customerId, e.getMessage());
        }
        
        log.info("Admin {} locked account for user {}. ", adminId, customerId);
        
        return customer;
    }
    
    @Transactional
    public User unlockAccount(String adminId, String customerId, String reason) {
        User customer = userService.unlockAccount(customerId);
        
        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("action", "UNLOCK_ACCOUNT");
        auditDetails.put("reason", reason);
        
        auditLogService.logAdminAction(adminId, customerId, "UNLOCK_ACCOUNT", auditDetails);
        
        try {
            notificationService.sendNotification(
                    customerId,
                    "Account Unlocked",
                    "Your account has been unlocked by an administrator.",
                    "admin_action"
            );
        } catch (Exception e) {
            log.warn("Failed to send account unlocked notification to user {}: {}", customerId, e.getMessage());
        }
        
        log.info("Admin {} unlocked account for user {}. ", adminId, customerId);
        
        return customer;
    }
    
    @Transactional
    public User deactivateAccount(String adminId, String customerId, String reason) {
        if (adminId != null && adminId.equals(customerId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN,
                "Administrators cannot deactivate their own account"
            );
        }
        User customer = userService.deactivateAccount(customerId);
        
        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("action", "DEACTIVATE_ACCOUNT");
        auditDetails.put("reason", reason);
        
        auditLogService.logAdminAction(adminId, customerId, "DEACTIVATE_ACCOUNT", auditDetails);
        
        // Disable user in Keycloak and logout all sessions (best-effort)
        try {
            keycloakAdminService.setUserEnabled(customerId, false);
            keycloakAdminService.logoutUserSessions(customerId);
        } catch (Exception e) {
            log.warn("Failed to disable/logout Keycloak user {} on deactivation: {}", customerId, e.getMessage());
        }

        try {
            notificationService.sendNotification(
                    customerId,
                    "Account Deactivated",
                    "Your account has been deactivated by an administrator.",
                    "admin_action"
            );
        } catch (Exception e) {
            log.warn("Failed to send account deactivated notification to user {}: {}", customerId, e.getMessage());
        }
        
        log.info("Admin {} deactivated account for user {}. ", adminId, customerId);
        
        return customer;
    }
    
    @Transactional
    public User reactivateAccount(String adminId, String customerId, String reason) {
        User customer = userService.reactivateAccount(customerId);
        
        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("action", "REACTIVATE_ACCOUNT");
        auditDetails.put("reason", reason);
        
        auditLogService.logAdminAction(adminId, customerId, "REACTIVATE_ACCOUNT", auditDetails);
        
        // Re-enable user in Keycloak (best-effort)
        try {
            keycloakAdminService.setUserEnabled(customerId, true);
        } catch (Exception e) {
            log.warn("Failed to enable Keycloak user {} on reactivation: {}", customerId, e.getMessage());
        }

        try {
            notificationService.sendNotification(
                    customerId,
                    "Account Reactivated",
                    "Your account has been reactivated by an administrator.",
                    "admin_action"
            );
        } catch (Exception e) {
            log.warn("Failed to send account reactivated notification to user {}: {}", customerId, e.getMessage());
        }
        
        log.info("Admin {} reactivated account for user {}. ", adminId, customerId);
        
        return customer;
    }
}
