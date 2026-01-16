package com.shophub.controller;

import com.shophub.config.TestSecurityConfig;
import com.shophub.model.Category;
import com.shophub.model.Order;
import com.shophub.model.Product;
import com.shophub.repository.CategoryRepository;
import com.shophub.repository.OrderRepository;
import com.shophub.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class WarehouseControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private String warehouseStaffUserId = "warehouse-user-123";
    private String customerUserId = "customer-user-456";

    @BeforeEach
    void setUp() {
        // Clean up
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    @Transactional
    void getAllOrders_returnsAllOrders_withWarehouseStaffRole() throws Exception {
        // Arrange
        Map<String, Object> shippingAddress = new HashMap<>();
        shippingAddress.put("fullName", "John Doe");
        shippingAddress.put("address", "123 Main St");

        Order order1 = Order.builder()
                .userId(customerUserId)
                .orderStatus("pending")
                .totalAmount(new BigDecimal("100.00"))
                .shippingCost(new BigDecimal("10.00"))
                .shippingAddress(shippingAddress)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        orderRepository.save(order1);

        Order order2 = Order.builder()
                .userId(customerUserId)
                .orderStatus("processing")
                .totalAmount(new BigDecimal("200.00"))
                .shippingCost(new BigDecimal("10.00"))
                .shippingAddress(shippingAddress)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        orderRepository.save(order2);

        // Act and Assert
        mockMvc.perform(get("/api/warehouse/orders")
                        .with(jwt().jwt(jwt -> jwt.subject(warehouseStaffUserId))
                                .authorities(new SimpleGrantedAuthority("ROLE_warehouse-staff")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @Transactional
    void getAllOrders_returnsEmptyList_whenNoOrders() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/warehouse/orders")
                        .with(jwt().jwt(jwt -> jwt.subject(warehouseStaffUserId))
                                .authorities(new SimpleGrantedAuthority("ROLE_warehouse-staff")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Transactional
    void getAllOrders_filtersOrdersByStatus_whenStatusProvided() throws Exception {
        // Arrange
        Map<String, Object> shippingAddress = new HashMap<>();
        shippingAddress.put("fullName", "John Doe");

        Order pendingOrder = Order.builder()
                .userId(customerUserId)
                .orderStatus("pending")
                .totalAmount(new BigDecimal("100.00"))
                .shippingCost(new BigDecimal("10.00"))
                .shippingAddress(shippingAddress)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        orderRepository.save(pendingOrder);

        Order processingOrder = Order.builder()
                .userId(customerUserId)
                .orderStatus("processing")
                .totalAmount(new BigDecimal("200.00"))
                .shippingCost(new BigDecimal("10.00"))
                .shippingAddress(shippingAddress)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        orderRepository.save(processingOrder);

        // Act and Assert - Filter by pending status
        mockMvc.perform(get("/api/warehouse/orders")
                        .param("status", "pending")
                        .with(jwt().jwt(jwt -> jwt.subject(warehouseStaffUserId))
                                .authorities(new SimpleGrantedAuthority("ROLE_warehouse-staff")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].order_status", is("pending")));
    }

    @Test
    @Transactional
    void getAllOrders_returns403_whenUserIsNotWarehouseStaff() throws Exception {
        // Act and Assert - Regular customer should not have access
        mockMvc.perform(get("/api/warehouse/orders")
                        .with(jwt().jwt(jwt -> jwt.subject(customerUserId))
                                .authorities(new SimpleGrantedAuthority("ROLE_customer")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void getAllOrdersDebug_returnsOrders_withAnyAuthenticatedUser() throws Exception {
        // Arrange
        Map<String, Object> shippingAddress = new HashMap<>();
        shippingAddress.put("fullName", "John Doe");

        Order order = Order.builder()
                .userId(customerUserId)
                .orderStatus("pending")
                .totalAmount(new BigDecimal("100.00"))
                .shippingCost(new BigDecimal("10.00"))
                .shippingAddress(shippingAddress)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        orderRepository.save(order);

        // Act and Assert - Any authenticated user can access debug endpoint
        mockMvc.perform(get("/api/warehouse/orders/all")
                        .with(jwt().jwt(jwt -> jwt.subject(customerUserId))
                                .authorities(new SimpleGrantedAuthority("ROLE_customer")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @Transactional
    void getAllOrdersDebug_filtersOrdersByStatus() throws Exception {
        // Arrange
        Map<String, Object> shippingAddress = new HashMap<>();
        shippingAddress.put("fullName", "John Doe");

        Order pendingOrder = Order.builder()
                .userId(customerUserId)
                .orderStatus("pending")
                .totalAmount(new BigDecimal("100.00"))
                .shippingCost(new BigDecimal("10.00"))
                .shippingAddress(shippingAddress)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        orderRepository.save(pendingOrder);

        Order shippedOrder = Order.builder()
                .userId(customerUserId)
                .orderStatus("shipped")
                .totalAmount(new BigDecimal("200.00"))
                .shippingCost(new BigDecimal("10.00"))
                .shippingAddress(shippingAddress)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        orderRepository.save(shippedOrder);

        // Act and Assert - Filter by shipped status
        mockMvc.perform(get("/api/warehouse/orders/all")
                        .param("status", "shipped")
                        .with(jwt().jwt(jwt -> jwt.subject(customerUserId))
                                .authorities(new SimpleGrantedAuthority("ROLE_customer")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].order_status", is("shipped")));
    }

    @Test
    @Transactional
    void getAllOrders_returnsOrdersWithAllFields() throws Exception {
        // Arrange
        Map<String, Object> shippingAddress = new HashMap<>();
        shippingAddress.put("fullName", "John Doe");
        shippingAddress.put("address", "123 Main St");
        shippingAddress.put("city", "Boston");

        Order order = Order.builder()
                .userId(customerUserId)
                .orderStatus("pending")
                .totalAmount(new BigDecimal("100.00"))
                .shippingCost(new BigDecimal("10.00"))
                .shippingAddress(shippingAddress)
                .trackingNumber("TRACK123")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        orderRepository.save(order);

        // Act and Assert
        mockMvc.perform(get("/api/warehouse/orders")
                        .with(jwt().jwt(jwt -> jwt.subject(warehouseStaffUserId))
                                .authorities(new SimpleGrantedAuthority("ROLE_warehouse-staff")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].order_id").exists())
                .andExpect(jsonPath("$[0].user_id").exists())
                .andExpect(jsonPath("$[0].order_status").exists())
                .andExpect(jsonPath("$[0].total_amount").exists())
                .andExpect(jsonPath("$[0].shipping_cost").exists())
                .andExpect(jsonPath("$[0].shipping_address").exists())
                .andExpect(jsonPath("$[0].tracking_number").exists());
    }
}
