package com.shophub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shophub.dto.AdminOrderUpdateRequest;
import com.shophub.dto.CreateOrderRequest;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.model.Order;
import com.shophub.model.OrderItem;
import com.shophub.model.Product;
import com.shophub.repository.OrderItemRepository;
import com.shophub.repository.OrderRepository;
import com.shophub.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final CartService cartService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    
    @Transactional(readOnly = true)
    public List<Order> getUserOrders(String userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        // Load items for each order with product details
        orders.forEach(order -> {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getOrderId());
            
            // Populate product details for each item
            items.forEach(item -> {
                productRepository.findById(item.getProductId()).ifPresent(product -> {
                    item.setProductName(product.getName());
                    item.setImages(product.getImages());
                });
            });
            
            order.setItems(items);
        });
        
        return orders;
    }
    
    @Transactional(readOnly = true)
    public Order getOrderById(String userId, Integer orderId) {
        Order order = orderRepository.findByOrderIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        
        // Populate product details for each item
        items.forEach(item -> {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                item.setProductName(product.getName());
                item.setImages(product.getImages());
            });
        });
        
        order.setItems(items);
        
        return order;
    }
    
    @Transactional
    public Order createOrder(String userId, CreateOrderRequest request, Jwt jwt) {
        log.info("Creating order for user: {}", userId);
        
        // Ensure user exists
        userService.getOrCreateUser(jwt);

        // Block locked or deactivated accounts from placing orders
        com.shophub.model.User user = userService.getUserById(userId);
        if (Boolean.TRUE.equals(user.getIsLocked())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account is locked and cannot place orders.");
        }
        if (Boolean.TRUE.equals(user.getIsDeactivated())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account is deactivated and cannot place orders.");
        }
        
        // Validate shipping address
        if (request.getShipping() == null || request.getShipping().getAddress() == null) {
            throw new IllegalArgumentException("Shipping address is required");
        }
        
        // Validate items
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        
        // Generate tracking number
        String trackingNumber = "TRK-" + System.currentTimeMillis() + "-" + 
                                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        // Convert shipping address to Map
        Map<String, Object> shippingAddressMap;
        try {
            String addressJson = objectMapper.writeValueAsString(request.getShipping().getAddress());
            shippingAddressMap = objectMapper.readValue(addressJson, Map.class);
        } catch (Exception e) {
            log.error("Failed to convert shipping address to JSON", e);
            throw new IllegalArgumentException("Invalid shipping address format");
        }
        
        // Create order
        Order order = Order.builder()
                .userId(userId)
                .orderStatus("pending")
                .totalAmount(request.getTotal())
                .shippingCost(request.getShipping().getCost() != null ? 
                             request.getShipping().getCost() : BigDecimal.ZERO)
                .shippingAddress(shippingAddressMap)
                .billingAddress(shippingAddressMap)
                .trackingNumber(trackingNumber)
                .shippingProvider("Standard Shipping")
                .estimatedDeliveryDate(LocalDate.now().plusDays(7))
                .build();
        
        order = orderRepository.save(order);
        log.info("Order created: {}", order.getOrderId());
        
        // Create order items
        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            Integer productId = item.getProductId() != null ? item.getProductId() : item.getId();
            
            if (productId == null) {
                throw new IllegalArgumentException("Product ID is required for all items");
            }
            
            // Get product to ensure it exists and has stock
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
            
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
            }
            
            BigDecimal unitPrice = item.getPrice() != null ? item.getPrice() : product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            
            OrderItem orderItem = OrderItem.builder()
                    .orderId(order.getOrderId())
                    .productId(productId)
                    .quantity(item.getQuantity())
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();
            
            orderItemRepository.save(orderItem);
            
            // Update product stock and trigger low/none stock alerts when needed
            int newStock = product.getStockQuantity() - item.getQuantity();
            product.setStockQuantity(newStock);
            productRepository.save(product);

            Integer threshold = product.getLowStockThreshold() != null ? product.getLowStockThreshold() : 10;
            if (newStock <= threshold) {
                log.warn("🚨 Low/none stock detected during order creation for product {}: stock={}, threshold={}",
                        productId, newStock, threshold);
                notificationService.sendLowStockAlert(product);
            }
            
            log.info("Added order item: Product {} x{}", productId, item.getQuantity());
        }
        
        // Clear user's cart after successful order
        try {
            cartService.clearCart(userId);
            log.info("Cleared cart for user: {}", userId);
        } catch (Exception e) {
            log.warn("Failed to clear cart for user: {}", userId, e);
        }
        
        log.info("Order completed successfully: {}", order.getOrderId());
        
        // Return order with items
        return getOrderById(userId, order.getOrderId());
    }
    
    @Transactional
    public Order updateOrderStatus(Integer orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        // Validate status
        List<String> validStatuses = List.of("pending", "processing", "shipped", "delivered", "cancelled");
        if (!validStatuses.contains(status)) {
            throw new IllegalArgumentException("Invalid order status: " + status);
        }
        
        String oldStatus = order.getOrderStatus();
        order.setOrderStatus(status);
        log.info("Updating order {} status from {} to {}", orderId, oldStatus, status);
        
        Order savedOrder = orderRepository.save(order);
        
        // Send notification to customer
        notificationService.sendOrderStatusNotification(savedOrder, oldStatus, status);
        
        return savedOrder;
    }
    
    /**
     * Admin confirms payment for an order and moves it to processing
     */
    @Transactional
    public Order confirmPayment(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
        
        if (!"pending".equals(order.getOrderStatus())) {
            throw new IllegalStateException("Only pending orders can have payment confirmed. Current status: " + order.getOrderStatus());
        }
        
        String oldStatus = order.getOrderStatus();
        order.setOrderStatus("processing");
        
        log.info("Admin confirmed payment for order {}", orderId);
        Order savedOrder = orderRepository.save(order);
        
        // Send payment confirmation notification
        notificationService.sendPaymentConfirmationNotification(savedOrder);
        notificationService.sendOrderStatusNotification(savedOrder, oldStatus, "processing");
        
        return savedOrder;
    }
    
    /**
     * Admin updates order with comprehensive details (status, tracking, etc.)
     */
    @Transactional
    public Order adminUpdateOrder(Integer orderId, AdminOrderUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
        
        String oldStatus = order.getOrderStatus();
        
        // Update status if provided
        if (request.getStatus() != null) {
            List<String> validStatuses = List.of("pending", "processing", "shipped", "delivered", "cancelled");
            if (!validStatuses.contains(request.getStatus())) {
                throw new IllegalArgumentException("Invalid order status: " + request.getStatus());
            }
            order.setOrderStatus(request.getStatus());
        }
        
        // Update tracking information if provided
        if (request.getTrackingNumber() != null && !request.getTrackingNumber().isBlank()) {
            order.setTrackingNumber(request.getTrackingNumber());
        }
        
        if (request.getShippingProvider() != null && !request.getShippingProvider().isBlank()) {
            order.setShippingProvider(request.getShippingProvider());
        }
        
        log.info("Admin updated order {}: status={}, tracking={}, provider={}", 
                orderId, request.getStatus(), request.getTrackingNumber(), request.getShippingProvider());
        
        Order savedOrder = orderRepository.save(order);
        
        // Send appropriate notifications
        if (!oldStatus.equals(savedOrder.getOrderStatus())) {
            notificationService.sendOrderStatusNotification(savedOrder, oldStatus, savedOrder.getOrderStatus());
        }
        
        return savedOrder;
    }
    
    /**
     * Warehouse staff marks order as shipped with tracking details
     */
    @Transactional
    public Order markAsShipped(Integer orderId, String trackingNumber, String shippingProvider) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
        
        if (!"processing".equals(order.getOrderStatus())) {
            throw new IllegalStateException("Only processing orders can be marked as shipped. Current status: " + order.getOrderStatus());
        }
        
        String oldStatus = order.getOrderStatus();
        order.setOrderStatus("shipped");
        
        if (trackingNumber != null && !trackingNumber.isBlank()) {
            order.setTrackingNumber(trackingNumber);
        }
        
        if (shippingProvider != null && !shippingProvider.isBlank()) {
            order.setShippingProvider(shippingProvider);
        }
        
        // Update estimated delivery date
        order.setEstimatedDeliveryDate(LocalDate.now().plusDays(5));
        
        log.info("Order {} marked as shipped. Tracking: {}, Provider: {}", 
                orderId, trackingNumber, shippingProvider);
        
        Order savedOrder = orderRepository.save(order);
        
        // Send status notification (includes tracking info)
        notificationService.sendOrderStatusNotification(savedOrder, oldStatus, "shipped");
        
        return savedOrder;
    }
    
    /**
     * Warehouse staff confirms delivery for an order
     */
    @Transactional
    public Order markAsDelivered(Integer orderId, boolean confirmationProvided) {
        if (!confirmationProvided) {
            throw new IllegalArgumentException("Delivery confirmation is required to mark the order as delivered.");
        }
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
        
        if (!"shipped".equals(order.getOrderStatus())) {
            throw new IllegalStateException("Only shipped orders can be marked as delivered. Current status: " + order.getOrderStatus());
        }
        
        String oldStatus = order.getOrderStatus();
        order.setOrderStatus("delivered");
        order.setEstimatedDeliveryDate(LocalDate.now());
        
        log.info("Order {} marked as delivered.", orderId);
        
        Order savedOrder = orderRepository.save(order);
        
        notificationService.sendOrderStatusNotification(savedOrder, oldStatus, "delivered");
        notificationService.sendDeliveryNotificationToStaff(savedOrder);
        
        return savedOrder;
    }
    
    @Transactional(readOnly = true)
    public List<Order> getAllOrders(String status, String search) {
        List<Order> orders = orderRepository.findAllWithFilters(status, search);
        
        // Load items for each order with product details and user information
        orders.forEach(order -> {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getOrderId());
            
            // Populate product details for each item
            items.forEach(item -> {
                productRepository.findById(item.getProductId()).ifPresent(product -> {
                    item.setProductName(product.getName());
                    item.setImages(product.getImages());
                });
            });
            
            order.setItems(items);
            
            // Populate user details (firstName and lastName)
            try {
                com.shophub.model.User user = userService.getUserById(order.getUserId());
                if (user != null) {
                    order.setFirstName(user.getFirstName());
                    order.setLastName(user.getLastName());
                    order.setEmail(user.getEmail());
                }
            } catch (Exception e) {
                log.warn("Failed to fetch user details for order: {}", order.getOrderId(), e);
            }
        });
        
        return orders;
    }
}
