package com.shophub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shophub.dto.CreateOrderRequest;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.model.Order;
import com.shophub.model.OrderItem;
import com.shophub.model.Product;
import com.shophub.repository.OrderItemRepository;
import com.shophub.repository.OrderRepository;
import com.shophub.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @Mock
    private CartService cartService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private OrderItem testOrderItem;
    private Product testProduct;
    private CreateOrderRequest testOrderRequest;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .productId(1)
                .name("Test Product")
                .price(new BigDecimal("99.99"))
                .stockQuantity(10)
                .images(Arrays.asList("image1.jpg"))
                .build();

        testOrderItem = OrderItem.builder()
                .orderItemId(1)
                .orderId(1)
                .productId(1)
                .quantity(2)
                .unitPrice(new BigDecimal("99.99"))
                .subtotal(new BigDecimal("199.98"))
                .build();

        testOrder = Order.builder()
                .orderId(1)
                .userId("user123")
                .orderStatus("pending")
                .totalAmount(new BigDecimal("199.98"))
                .shippingCost(new BigDecimal("10.00"))
                .trackingNumber("TRK-123456789")
                .estimatedDeliveryDate(LocalDate.now().plusDays(7))
                .build();

        CreateOrderRequest.ShippingInfo shippingInfo = new CreateOrderRequest.ShippingInfo(
                new CreateOrderRequest.Address("John Doe", "123 Main St", "Test City", "12345", "1234567890"),
                new BigDecimal("10.00")
        );

        CreateOrderRequest.OrderItemRequest itemRequest = new CreateOrderRequest.OrderItemRequest(
                1, null, 2, new BigDecimal("99.99"), "Test Product"
        );

        testOrderRequest = CreateOrderRequest.builder()
                .total(new BigDecimal("199.98"))
                .shipping(shippingInfo)
                .items(Arrays.asList(itemRequest))
                .build();
    }

    @Test
    void getUserOrders_ShouldReturnOrdersWithItems() {
        // Given
        List<Order> orders = Arrays.asList(testOrder);
        List<OrderItem> items = Arrays.asList(testOrderItem);
        when(orderRepository.findByUserIdOrderByCreatedAtDesc("user123")).thenReturn(orders);
        when(orderItemRepository.findByOrderId(1)).thenReturn(items);
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));

        // When
        List<Order> result = orderService.getUserOrders("user123");

        // Then
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getItems().size());
        assertEquals("Test Product", result.get(0).getItems().get(0).getProductName());
        verify(orderRepository).findByUserIdOrderByCreatedAtDesc("user123");
        verify(orderItemRepository).findByOrderId(1);
        verify(productRepository).findById(1);
    }

    @Test
    void getOrderById_ShouldReturnOrderWithItems() {
        // Given
        List<OrderItem> items = Arrays.asList(testOrderItem);
        when(orderRepository.findByOrderIdAndUserId(1, "user123")).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findByOrderId(1)).thenReturn(items);
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));

        // When
        Order result = orderService.getOrderById("user123", 1);

        // Then
        assertEquals("pending", result.getOrderStatus());
        assertEquals(1, result.getItems().size());
        assertEquals("Test Product", result.getItems().get(0).getProductName());
        verify(orderRepository).findByOrderIdAndUserId(1, "user123");
        verify(orderItemRepository).findByOrderId(1);
        verify(productRepository).findById(1);
    }

    @Test
    void getOrderById_ShouldThrowExceptionWhenOrderNotFound() {
        // Given
        when(orderRepository.findByOrderIdAndUserId(1, "user123")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderById("user123", 1));
        verify(orderRepository).findByOrderIdAndUserId(1, "user123");
    }

    @Test
    void createOrder_ShouldCreateOrderSuccessfully() throws Exception {
        // Given
        when(userService.getOrCreateUser(jwt)).thenReturn(null);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(objectMapper.readValue(anyString(), eq(java.util.Map.class))).thenReturn(java.util.Map.of());
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(testOrderItem);
        when(orderRepository.findByOrderIdAndUserId(1, "user123")).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findByOrderId(1)).thenReturn(Arrays.asList(testOrderItem));

        // When
        Order result = orderService.createOrder("user123", testOrderRequest, jwt);

        // Then
        assertNotNull(result);
        verify(userService).getOrCreateUser(jwt);
        verify(orderRepository).save(any(Order.class));
        verify(productRepository, atLeastOnce()).findById(1); // Can be called multiple times
        verify(orderItemRepository).save(any(OrderItem.class));
        verify(cartService).clearCart("user123");
    }

    @Test
    void createOrder_ShouldThrowExceptionWhenShippingAddressMissing() {
        // Given
        testOrderRequest.setShipping(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder("user123", testOrderRequest, jwt));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_ShouldThrowExceptionWhenItemsEmpty() {
        // Given
        testOrderRequest.setItems(Arrays.asList());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder("user123", testOrderRequest, jwt));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_ShouldThrowExceptionWhenProductNotFound() throws Exception {
        // Given
        when(userService.getOrCreateUser(jwt)).thenReturn(null);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(objectMapper.readValue(anyString(), eq(java.util.Map.class))).thenReturn(java.util.Map.of());
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(productRepository.findById(1)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> orderService.createOrder("user123", testOrderRequest, jwt));
        verify(productRepository).findById(1);
        verify(orderItemRepository, never()).save(any(OrderItem.class));
    }

    @Test
    void createOrder_ShouldThrowExceptionWhenInsufficientStock() throws Exception {
        // Given
        testProduct.setStockQuantity(1);
        when(userService.getOrCreateUser(jwt)).thenReturn(null);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(objectMapper.readValue(anyString(), eq(java.util.Map.class))).thenReturn(java.util.Map.of());
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder("user123", testOrderRequest, jwt));
        verify(productRepository).findById(1);
        verify(orderItemRepository, never()).save(any(OrderItem.class));
    }

    @Test
    void updateOrderStatus_ShouldUpdateStatusSuccessfully() {
        // Given
        when(orderRepository.findById(1)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        Order result = orderService.updateOrderStatus(1, "shipped");

        // Then
        assertEquals("shipped", result.getOrderStatus());
        verify(orderRepository).findById(1);
        verify(orderRepository).save(testOrder);
    }

    @Test
    void updateOrderStatus_ShouldThrowExceptionWhenOrderNotFound() {
        // Given
        when(orderRepository.findById(1)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> orderService.updateOrderStatus(1, "shipped"));
        verify(orderRepository).findById(1);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_ShouldThrowExceptionWhenInvalidStatus() {
        // Given
        when(orderRepository.findById(1)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> orderService.updateOrderStatus(1, "invalid"));
        verify(orderRepository).findById(1);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getAllOrders_ShouldReturnOrdersWithItems() {
        // Given
        List<Order> orders = Arrays.asList(testOrder);
        List<OrderItem> items = Arrays.asList(testOrderItem);
        when(orderRepository.findAllWithFilters(null, null)).thenReturn(orders);
        when(orderItemRepository.findByOrderId(1)).thenReturn(items);
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));

        // When
        List<Order> result = orderService.getAllOrders(null, null);

        // Then
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getItems().size());
        assertEquals("Test Product", result.get(0).getItems().get(0).getProductName());
        verify(orderRepository).findAllWithFilters(null, null);
        verify(orderItemRepository).findByOrderId(1);
        verify(productRepository).findById(1);
    }
}
