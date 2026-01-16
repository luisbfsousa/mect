package com.shophub.controller;

import com.shophub.dto.CreateOrderRequest;
import com.shophub.model.Order;
import com.shophub.service.OrderService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private OrderController controller;

    private String testUserId;
    private Order sampleOrder;
    private CreateOrderRequest createRequest;

    @BeforeEach
    void setUp() {
        testUserId = "user-789";
        
        // Sample order
        sampleOrder = Order.builder()
                .orderId(1)
                .userId(testUserId)
                .orderStatus("pending")
                .totalAmount(new BigDecimal("149.99"))
                .shippingCost(new BigDecimal("10.00"))
                .createdAt(LocalDateTime.now())
                .build();

        // Create order request
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(101);
        item.setQuantity(2);
        item.setPrice(new BigDecimal("69.99"));
        item.setProductName("Keyboard");

        CreateOrderRequest.Address address = new CreateOrderRequest.Address();
        address.setFullName("Jane Smith");
        address.setAddress("789 Elm St");
        address.setCity("Boston");

        CreateOrderRequest.ShippingInfo shipping = new CreateOrderRequest.ShippingInfo();
        shipping.setAddress(address);
        shipping.setCost(new BigDecimal("10.00"));

        createRequest = CreateOrderRequest.builder()
                .items(Arrays.asList(item))
                .total(new BigDecimal("149.99"))
                .shipping(shipping)
                .build();
    }

    /**
     * Test: Create a new order
     */
    @Test
    void createOrder_ShouldReturnCreatedOrder_WhenValidRequest() {
        // Given
        when(jwt.getSubject()).thenReturn(testUserId);
        when(orderService.createOrder(eq(testUserId), any(CreateOrderRequest.class), eq(jwt)))
                .thenReturn(sampleOrder);

        // When
        ResponseEntity<Order> response = controller.createOrder(jwt, createRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getOrderId());
        assertEquals("pending", response.getBody().getOrderStatus());
        assertEquals(new BigDecimal("149.99"), response.getBody().getTotalAmount());
        
        verify(orderService, times(1)).createOrder(eq(testUserId), any(CreateOrderRequest.class), eq(jwt));
    }

    /**
     * Test: Get all orders for a user
     */
    @Test
    void getUserOrders_ShouldReturnOrderList_WhenOrdersExist() {
        // Given
        Order order2 = Order.builder()
                .orderId(2)
                .userId(testUserId)
                .orderStatus("shipped")
                .totalAmount(new BigDecimal("299.99"))
                .build();
        
        List<Order> orders = Arrays.asList(sampleOrder, order2);
        when(jwt.getSubject()).thenReturn(testUserId);
        when(orderService.getUserOrders(testUserId)).thenReturn(orders);

        // When
        ResponseEntity<List<Order>> response = controller.getUserOrders(jwt);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        assertEquals("pending", response.getBody().get(0).getOrderStatus());
        assertEquals("shipped", response.getBody().get(1).getOrderStatus());
        
        verify(orderService, times(1)).getUserOrders(testUserId);
    }

    /**
     * Test: Get a specific order by ID
     */
    @Test
    void getOrderById_ShouldReturnOrder_WhenOrderExists() {
        // Given
        when(jwt.getSubject()).thenReturn(testUserId);
        when(orderService.getOrderById(testUserId, 1)).thenReturn(sampleOrder);

        // When
        ResponseEntity<Order> response = controller.getOrderById(jwt, 1);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getOrderId());
        assertEquals(testUserId, response.getBody().getUserId());
        assertEquals("pending", response.getBody().getOrderStatus());
        
        verify(orderService, times(1)).getOrderById(testUserId, 1);
    }

    /**
     * Test: Create order with multiple items
     */
    @Test
    void createOrder_ShouldHandleMultipleItems() {
        // Given
        CreateOrderRequest.OrderItemRequest item1 = new CreateOrderRequest.OrderItemRequest();
        item1.setProductId(101);
        item1.setQuantity(1);
        
        CreateOrderRequest.OrderItemRequest item2 = new CreateOrderRequest.OrderItemRequest();
        item2.setProductId(102);
        item2.setQuantity(3);
        
        CreateOrderRequest multiItemRequest = CreateOrderRequest.builder()
                .items(Arrays.asList(item1, item2))
                .total(new BigDecimal("299.99"))
                .shipping(createRequest.getShipping())
                .build();
        
        when(jwt.getSubject()).thenReturn(testUserId);
        when(orderService.createOrder(eq(testUserId), any(), eq(jwt)))
                .thenReturn(sampleOrder);

        // When
        ResponseEntity<Order> response = controller.createOrder(jwt, multiItemRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(orderService, times(1)).createOrder(eq(testUserId), any(), eq(jwt));
    }
}
