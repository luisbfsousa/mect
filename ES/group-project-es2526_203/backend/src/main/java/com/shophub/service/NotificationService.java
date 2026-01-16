package com.shophub.service;

import com.shophub.dto.OrderNotification;
import com.shophub.model.Notification;
import com.shophub.model.Order;
import com.shophub.model.Product;
import com.shophub.model.User;
import com.shophub.repository.NotificationRepository;
import com.shophub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for handling customer notifications.
 * In a production environment, this would integrate with an email service (e.g., SendGrid, AWS SES).
 * For now, it logs notifications that would be sent to customers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    
    private static final List<String> DELIVERY_STAFF_ROLES = List.of("content-manager", "administrator");
    private static final List<String> INVENTORY_ALERT_ROLES = List.of(
            "administrator",
            "admin" // allow shorthand if used
    );
    
    /**
     * Send order status update notification to customer
     */
    @Transactional
    public void sendOrderStatusNotification(Order order, String oldStatus, String newStatus) {
        try {
            // Get user details
            User user = userRepository.findById(order.getUserId()).orElse(null);
            
            OrderNotification notification = OrderNotification.builder()
                    .orderId(order.getOrderId())
                    .userId(order.getUserId())
                    .customerEmail(user != null ? user.getEmail() : "unknown@example.com")
                    .customerName(user != null ? (user.getFirstName() + " " + user.getLastName()) : "Customer")
                    .oldStatus(oldStatus)
                    .newStatus(newStatus)
                    .trackingNumber(order.getTrackingNumber())
                    .shippingProvider(order.getShippingProvider())
                    .totalAmount(order.getTotalAmount())
                    .updatedAt(LocalDateTime.now())
                    .message(generateStatusMessage(newStatus, order))
                    .build();
            
            // Save notification to database
            createNotification(
                order.getUserId(),
                order.getOrderId(),
                "Order Status Updated",
                notification.getMessage(),
                "order_status"
            );
            
            // Log the notification (in production, this would send an actual email)
            log.info("📧 ORDER NOTIFICATION:");
            log.info("   To: {} ({})", notification.getCustomerName(), notification.getCustomerEmail());
            log.info("   Subject: Order #{} Status Update", notification.getOrderId());
            log.info("   Order Status: {} → {}", oldStatus, newStatus);
            log.info("   Message: {}", notification.getMessage());
            
            if (order.getTrackingNumber() != null) {
                log.info("   Tracking: {} ({})", order.getTrackingNumber(), order.getShippingProvider());
            }
            
            log.info("   ✅ Notification sent successfully");
            
        } catch (Exception e) {
            log.error("Failed to send notification for order {}: {}", order.getOrderId(), e.getMessage(), e);
        }
    }

    /**
     * Send a generic notification to a user (admin actions, etc.).
     * This stores the notification in the database with no associated order.
     */
    @Transactional
    public void sendNotification(String userId, String title, String message, String type) {
        try {
            createNotification(
                userId,
                null,
                title,
                message,
                type
            );

            log.info("🔔 Generic notification stored: userId={}, title={}, type={}", userId, title, type);
        } catch (Exception e) {
            log.error("Failed to send generic notification to user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Notify internal staff members when an order is delivered.
     */
    @Transactional
    public void sendDeliveryNotificationToStaff(Order order) {
        try {
            List<User> staffMembers = userRepository.findByRoleIn(DELIVERY_STAFF_ROLES);
            
            if (staffMembers == null || staffMembers.isEmpty()) {
                log.info("No staff members found for delivery notification roles {}", DELIVERY_STAFF_ROLES);
                return;
            }
            
            User customer = userRepository.findById(order.getUserId()).orElse(null);
            String customerName = customer != null
                ? String.format("%s %s", 
                    customer.getFirstName() != null ? customer.getFirstName() : "",
                    customer.getLastName() != null ? customer.getLastName() : "")
                    .trim()
                : order.getUserId();
            
            if (customerName == null || customerName.isBlank()) {
                customerName = order.getUserId();
            }
            
            String message = String.format(
                "Order #%d for %s has been marked as delivered.",
                order.getOrderId(),
                customerName
            );
            
            if (order.getTrackingNumber() != null && !order.getTrackingNumber().isBlank()) {
                message += String.format(" Tracking number: %s.", order.getTrackingNumber());
            }
            
            for (User staff : staffMembers) {
                createNotification(
                    staff.getUserId(),
                    order.getOrderId(),
                    "Order Delivered",
                    message,
                    "order_delivery"
                );
                
                log.info("📦 Staff delivery notification stored for {} ({}) about order #{}",
                        staff.getEmail(), staff.getRole(), order.getOrderId());
            }
        } catch (Exception e) {
            log.error("Failed to notify staff about delivery for order {}: {}", 
                    order.getOrderId(), e.getMessage(), e);
        }
    }
    
    /**
     * Send payment confirmation notification
     */
    @Transactional
    public void sendPaymentConfirmationNotification(Order order) {
        try {
            User user = userRepository.findById(order.getUserId()).orElse(null);
            
            String message = "Your payment has been confirmed and your order is being processed.";
            
            // Add tracking info if available
            if (order.getTrackingNumber() != null && !order.getTrackingNumber().isBlank()) {
                message += String.format(" Tracking: %s", order.getTrackingNumber());
            }
            
            // Save notification to database
            createNotification(
                order.getUserId(),
                order.getOrderId(),
                "Payment Confirmed",
                message,
                "payment"
            );
            
            log.info("💳 PAYMENT CONFIRMATION:");
            log.info("   To: {} ({})", 
                    user != null ? (user.getFirstName() + " " + user.getLastName()) : "Customer",
                    user != null ? user.getEmail() : "unknown@example.com");
            log.info("   Order: #{}", order.getOrderId());
            log.info("   Amount: ${}", order.getTotalAmount());
            log.info("   Message: {}", message);
            log.info("   ✅ Payment confirmation sent successfully");
            
        } catch (Exception e) {
            log.error("Failed to send payment confirmation for order {}: {}", 
                    order.getOrderId(), e.getMessage(), e);
        }
    }

    /**
     * Send low-stock alert to administrators.
     */
    @Transactional
    public void sendLowStockAlert(Product product) {
        int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        int threshold = product.getLowStockThreshold() != null ? product.getLowStockThreshold() : 10;
        boolean outOfStock = stock <= 0;
        boolean lowStock = stock > 0 && stock <= threshold;

        // Only create notifications for low or no stock
        if (!outOfStock && !lowStock) {
            log.info("Stock OK for product {} (ID: {}) - stock={}, threshold={}, skipping alert",
                    product.getName(), product.getProductId(), stock, threshold);
            return;
        }

        log.warn("🚨 {} ALERT TRIGGERED for product: {} (ID: {}, Stock: {}, Threshold: {})",
                outOfStock ? "OUT OF STOCK" : "LOW STOCK",
                product.getName(), product.getProductId(), stock, threshold);

        try {
            // Find administrators only for inventory alerts
            List<User> admins = userRepository.findByRoleIn(INVENTORY_ALERT_ROLES);

            log.warn("🔍 Found {} inventory alert recipients in database", admins != null ? admins.size() : 0);

            if (admins == null || admins.isEmpty()) {
                log.error("❌ NO STAFF FOUND to notify about inventory status for product {}", product.getProductId());
                log.error("❌ Check your database - make sure users have role in {}", INVENTORY_ALERT_ROLES);
                return;
            }

            String title;
            String message;
            String type;

            if (outOfStock) {
                title = "Out of Stock Alert";
                message = String.format(
                    "Product '%s' (ID #%d) is out of stock.",
                    product.getName(),
                    product.getProductId()
                );
                type = "inventory_out_of_stock";
            } else {
                title = "Low/No Stock Alert";
                message = String.format(
                    "Product '%s' (ID #%d) is low on stock: %d units (threshold: %d).",
                    product.getName(),
                    product.getProductId(),
                    stock,
                    threshold
                );
                type = "inventory_low_stock";
            }

            for (User admin : admins) {
                log.warn("📧 Creating notification for admin: {} (ID: {}, Email: {})",
                        admin.getUserId(), admin.getUserId(), admin.getEmail());

                createNotification(
                        admin.getUserId(),
                        null, // not tied to an order
                        title,
                        message,
                        type
                );

                log.warn("✅ Notification created successfully for user {}", admin.getUserId());
            }

            log.warn("✅ INVENTORY ALERT COMPLETED - Created {} notifications", admins.size());
        } catch (Exception e) {
            log.error("❌ FAILED to send low-stock alerts for product {}: {}", product.getProductId(), e.getMessage(), e);
            e.printStackTrace();
        }
    }
    
    /**
     * Generate appropriate message based on order status
     */
    private String generateStatusMessage(String status, Order order) {
        String baseMessage = switch (status.toLowerCase()) {
            case "pending" -> "Your order has been received and is awaiting payment confirmation.";
            case "processing" -> "Your payment has been confirmed and your order is being prepared for shipment.";
            case "shipped" -> "Great news! Your order has been shipped and is on its way to you.";
            case "delivered" -> "Your order has been delivered successfully. We hope you enjoy your purchase!";
            case "cancelled" -> "Your order has been cancelled. If you have any questions, please contact support.";
            default -> "Your order status has been updated.";
        };
        
        // Add tracking information if available
        if (order.getTrackingNumber() != null && !order.getTrackingNumber().isBlank()) {
            baseMessage += String.format(" Tracking: %s", order.getTrackingNumber());
            // Add delivery date for shipped orders
            if (order.getEstimatedDeliveryDate() != null && "shipped".equalsIgnoreCase(status)) {
                baseMessage += String.format(". Expected delivery: %s", order.getEstimatedDeliveryDate());
            }
        }
        
        return baseMessage;
    }
    
    /**
     * Create and save a notification to database
     */
    private void createNotification(String userId, Integer orderId, String title, String message, String type) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setOrderId(orderId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }
}
