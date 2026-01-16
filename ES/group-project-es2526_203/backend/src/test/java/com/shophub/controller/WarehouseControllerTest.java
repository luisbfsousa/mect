package com.shophub.controller;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private WarehouseController controller;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = Order.builder()
                .orderId(1)
                .userId("user-123")
                .orderStatus("SHIPPED")
                .totalAmount(new BigDecimal("123.45"))
                .estimatedDeliveryDate(LocalDate.now().plusDays(3))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAllOrders_ShouldReturnOrdersList_WhenOrdersExist() {
        // Given
        List<Order> orders = Arrays.asList(sampleOrder);
        when(orderService.getAllOrders(null, null)).thenReturn(orders);

        // When
        ResponseEntity<List<Order>> response = controller.getAllOrders(null, null);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("SHIPPED", response.getBody().get(0).getOrderStatus());

        verify(orderService, times(1)).getAllOrders(null, null);
    }

    @Test
    void getAllOrdersDebug_ShouldReturnFilteredOrders_WhenParamsProvided() {
        // Given
        List<Order> orders = Arrays.asList(sampleOrder);
        when(orderService.getAllOrders("SHIPPED", "customer1")).thenReturn(orders);

        // When
        ResponseEntity<List<Order>> response = controller.getAllOrdersDebug("SHIPPED", "customer1");

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("user-123", response.getBody().get(0).getUserId());

        verify(orderService, times(1)).getAllOrders("SHIPPED", "customer1");
    }
}
