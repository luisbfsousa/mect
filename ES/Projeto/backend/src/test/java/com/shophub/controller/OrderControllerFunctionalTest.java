package com.shophub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shophub.config.TestSecurityConfig;
import com.shophub.dto.CreateOrderRequest;
import com.shophub.dto.UpdateOrderStatusRequest;
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
import java.util.Arrays;
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
public class OrderControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Product testProduct1;
    private Product testProduct2;
    private String testUserId = "test-user-123";
    private String otherUserId = "other-user-456";

    @BeforeEach
    void setUp() {
        // Clean up
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        // Create test category
        Category category = Category.builder()
                .name("Electronics")
                .description("Electronic items")
                .build();
        category = categoryRepository.save(category);

        // Create test products
        testProduct1 = Product.builder()
                .name("Laptop")
                .description("Gaming Laptop")
                .price(new BigDecimal("999.99"))
                .stockQuantity(10)
                .categoryId(category.getCategoryId())
                .build();
        testProduct1 = productRepository.save(testProduct1);

        testProduct2 = Product.builder()
                .name("Mouse")
                .description("Wireless Mouse")
                .price(new BigDecimal("29.99"))
                .stockQuantity(50)
                .categoryId(category.getCategoryId())
                .build();
        testProduct2 = productRepository.save(testProduct2);
    }

    @Test
    @Transactional
    void getUserOrders_returnsEmptyList_whenNoOrders() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/orders")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Transactional
    void getUserOrders_returnsUserOrders_whenOrdersExist() throws Exception {
        // Arrange
        Map<String, Object> shippingAddress = new HashMap<>();
        shippingAddress.put("fullName", "John Doe");
        shippingAddress.put("address", "123 Main St");
        shippingAddress.put("city", "Boston");
        shippingAddress.put("postalCode", "02101");

        Order order1 = Order.builder()
                .userId(testUserId)
                .orderStatus("pending")
                .totalAmount(new BigDecimal("999.99"))
                .shippingCost(new BigDecimal("10.00"))
                .shippingAddress(shippingAddress)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        orderRepository.save(order1);

        Order order2 = Order.builder()
                .userId(testUserId)
                .orderStatus("delivered")
                .totalAmount(new BigDecimal("29.99"))
                .shippingCost(new BigDecimal("5.00"))
                .shippingAddress(shippingAddress)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();
        orderRepository.save(order2);

        // Act and Assert
        mockMvc.perform(get("/api/orders")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].user_id", is(testUserId)))
                .andExpect(jsonPath("$[1].user_id", is(testUserId)));
    }

    @Test
    @Transactional
    void getUserOrders_isolatesUserData_doesNotReturnOtherUsersOrders() throws Exception {
        // Arrange
        Map<String, Object> shippingAddress = new HashMap<>();
        shippingAddress.put("fullName", "John Doe");
        shippingAddress.put("address", "123 Main St");

        Order userOrder = Order.builder()
                .userId(testUserId)
                .orderStatus("pending")
                .totalAmount(new BigDecimal("100.00"))
                .shippingCost(new BigDecimal("10.00"))
                .shippingAddress(shippingAddress)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        orderRepository.save(userOrder);

        Order otherUserOrder = Order.builder()
                .userId(otherUserId)
                .orderStatus("pending")
                .totalAmount(new BigDecimal("200.00"))
                .shippingCost(new BigDecimal("10.00"))
                .shippingAddress(shippingAddress)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        orderRepository.save(otherUserOrder);

        // Act and Assert
        mockMvc.perform(get("/api/orders")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].user_id", is(testUserId)))
                .andExpect(jsonPath("$[0].total_amount", is(100.00)));
    }

    @Test
    @Transactional
    void getUserOrders_requiresAuthentication() throws Exception {
        // Act and Assert - without JWT
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void getOrderById_returnsOrder_whenOrderBelongsToUser() throws Exception {
        // Arrange
        Map<String, Object> shippingAddress = new HashMap<>();
        shippingAddress.put("fullName", "John Doe");
        shippingAddress.put("address", "123 Main St");

        Order order = Order.builder()
                .userId(testUserId)
                .orderStatus("pending")
                .totalAmount(new BigDecimal("999.99"))
                .shippingCost(new BigDecimal("10.00"))
                .shippingAddress(shippingAddress)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        order = orderRepository.save(order);

        // Act and Assert
        mockMvc.perform(get("/api/orders/" + order.getOrderId())
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order_id", is(order.getOrderId())))
                .andExpect(jsonPath("$.user_id", is(testUserId)))
                .andExpect(jsonPath("$.order_status", is("pending")))
                .andExpect(jsonPath("$.total_amount", is(999.99)));
    }

    @Test
    @Transactional
    void getOrderById_returns404_whenOrderDoesNotExist() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/orders/99999")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId))))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void getOrderById_returns404_whenOrderBelongsToOtherUser() throws Exception {
        // Arrange
        Map<String, Object> shippingAddress = new HashMap<>();
        shippingAddress.put("fullName", "Jane Doe");
        shippingAddress.put("address", "456 Oak St");

        Order otherUserOrder = Order.builder()
                .userId(otherUserId)
                .orderStatus("pending")
                .totalAmount(new BigDecimal("100.00"))
                .shippingCost(new BigDecimal("10.00"))
                .shippingAddress(shippingAddress)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        otherUserOrder = orderRepository.save(otherUserOrder);

        // Act and Assert - testUserId trying to access otherUserId's order
        mockMvc.perform(get("/api/orders/" + otherUserOrder.getOrderId())
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId))))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void getOrderById_requiresAuthentication() throws Exception {
        // Arrange
        Map<String, Object> shippingAddress = new HashMap<>();
        shippingAddress.put("fullName", "John Doe");

        Order order = Order.builder()
                .userId(testUserId)
                .orderStatus("pending")
                .totalAmount(new BigDecimal("100.00"))
                .shippingCost(new BigDecimal("10.00"))
                .shippingAddress(shippingAddress)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        order = orderRepository.save(order);

        // Act and Assert - without JWT
        mockMvc.perform(get("/api/orders/" + order.getOrderId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void createOrder_createsOrder_withValidData() throws Exception {
        // Arrange
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(testProduct1.getProductId());
        item.setQuantity(1);
        item.setPrice(testProduct1.getPrice());
        item.setProductName(testProduct1.getName());

        CreateOrderRequest.Address address = new CreateOrderRequest.Address();
        address.setFullName("John Doe");
        address.setAddress("123 Main St");
        address.setCity("Boston");
        address.setPostalCode("02101");
        address.setPhone("555-1234");

        CreateOrderRequest.ShippingInfo shipping = new CreateOrderRequest.ShippingInfo();
        shipping.setAddress(address);
        shipping.setCost(new BigDecimal("10.00"));

        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(Arrays.asList(item))
                .total(new BigDecimal("1009.99"))
                .shipping(shipping)
                .build();

        String requestJson = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId)
                                .claim("email", "test@example.com")
                                .claim("given_name", "John")
                                .claim("family_name", "Doe")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.order_id").exists())
                .andExpect(jsonPath("$.user_id", is(testUserId)))
                .andExpect(jsonPath("$.order_status", is("pending")))
                .andExpect(jsonPath("$.total_amount").exists())
                .andExpect(jsonPath("$.shipping_address").exists());
    }

    @Test
    @Transactional
    void createOrder_returns400_whenItemsAreMissing() throws Exception {
        // Arrange
        CreateOrderRequest.Address address = new CreateOrderRequest.Address();
        address.setFullName("John Doe");
        address.setAddress("123 Main St");

        CreateOrderRequest.ShippingInfo shipping = new CreateOrderRequest.ShippingInfo();
        shipping.setAddress(address);
        shipping.setCost(new BigDecimal("10.00"));

        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(null) // Missing items
                .total(new BigDecimal("100.00"))
                .shipping(shipping)
                .build();

        String requestJson = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void createOrder_returns400_whenItemsAreEmpty() throws Exception {
        // Arrange
        CreateOrderRequest.Address address = new CreateOrderRequest.Address();
        address.setFullName("John Doe");
        address.setAddress("123 Main St");

        CreateOrderRequest.ShippingInfo shipping = new CreateOrderRequest.ShippingInfo();
        shipping.setAddress(address);
        shipping.setCost(new BigDecimal("10.00"));

        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(Arrays.asList()) // Empty items list
                .total(new BigDecimal("100.00"))
                .shipping(shipping)
                .build();

        String requestJson = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void createOrder_returns400_whenTotalIsMissing() throws Exception {
        // Arrange
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(testProduct1.getProductId());
        item.setQuantity(1);
        item.setPrice(testProduct1.getPrice());

        CreateOrderRequest.Address address = new CreateOrderRequest.Address();
        address.setFullName("John Doe");
        address.setAddress("123 Main St");

        CreateOrderRequest.ShippingInfo shipping = new CreateOrderRequest.ShippingInfo();
        shipping.setAddress(address);
        shipping.setCost(new BigDecimal("10.00"));

        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(Arrays.asList(item))
                .total(null) // Missing total
                .shipping(shipping)
                .build();

        String requestJson = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void createOrder_returns400_whenShippingInfoIsMissing() throws Exception {
        // Arrange
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(testProduct1.getProductId());
        item.setQuantity(1);
        item.setPrice(testProduct1.getPrice());

        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(Arrays.asList(item))
                .total(new BigDecimal("100.00"))
                .shipping(null) // Missing shipping
                .build();

        String requestJson = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void createOrder_requiresAuthentication() throws Exception {
        // Arrange
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(testProduct1.getProductId());
        item.setQuantity(1);
        item.setPrice(testProduct1.getPrice());

        CreateOrderRequest.Address address = new CreateOrderRequest.Address();
        address.setFullName("John Doe");
        address.setAddress("123 Main St");

        CreateOrderRequest.ShippingInfo shipping = new CreateOrderRequest.ShippingInfo();
        shipping.setAddress(address);
        shipping.setCost(new BigDecimal("10.00"));

        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(Arrays.asList(item))
                .total(new BigDecimal("100.00"))
                .shipping(shipping)
                .build();

        String requestJson = objectMapper.writeValueAsString(request);

        // Act and Assert - without JWT
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void updateOrderStatus_updatesStatus_withValidStatus() throws Exception {
        // Arrange
        Map<String, Object> shippingAddress = new HashMap<>();
        shippingAddress.put("fullName", "John Doe");
        shippingAddress.put("address", "123 Main St");

        Order order = Order.builder()
                .userId(testUserId)
                .orderStatus("pending")
                .totalAmount(new BigDecimal("100.00"))
                .shippingCost(new BigDecimal("10.00"))
                .shippingAddress(shippingAddress)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        order = orderRepository.save(order);

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus("processing");

        String requestJson = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(patch("/api/orders/" + order.getOrderId() + "/status")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order_status", is("processing")));
    }

    @Test
    @Transactional
    void updateOrderStatus_returns400_whenStatusIsInvalid() throws Exception {
        // Arrange
        Map<String, Object> shippingAddress = new HashMap<>();
        shippingAddress.put("fullName", "John Doe");

        Order order = Order.builder()
                .userId(testUserId)
                .orderStatus("pending")
                .totalAmount(new BigDecimal("100.00"))
                .shippingCost(new BigDecimal("10.00"))
                .shippingAddress(shippingAddress)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        order = orderRepository.save(order);

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus("invalid_status");

        String requestJson = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(patch("/api/orders/" + order.getOrderId() + "/status")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void updateOrderStatus_returns400_whenStatusIsMissing() throws Exception {
        // Arrange
        Map<String, Object> shippingAddress = new HashMap<>();
        shippingAddress.put("fullName", "John Doe");

        Order order = Order.builder()
                .userId(testUserId)
                .orderStatus("pending")
                .totalAmount(new BigDecimal("100.00"))
                .shippingCost(new BigDecimal("10.00"))
                .shippingAddress(shippingAddress)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        order = orderRepository.save(order);

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(null); // Missing status

        String requestJson = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(patch("/api/orders/" + order.getOrderId() + "/status")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void updateOrderStatus_returns404_whenOrderDoesNotExist() throws Exception {
        // Arrange
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus("processing");

        String requestJson = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(patch("/api/orders/99999/status")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void updateOrderStatus_requiresAuthentication() throws Exception {
        // Arrange
        Map<String, Object> shippingAddress = new HashMap<>();
        shippingAddress.put("fullName", "John Doe");

        Order order = Order.builder()
                .userId(testUserId)
                .orderStatus("pending")
                .totalAmount(new BigDecimal("100.00"))
                .shippingCost(new BigDecimal("10.00"))
                .shippingAddress(shippingAddress)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        order = orderRepository.save(order);

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus("processing");

        String requestJson = objectMapper.writeValueAsString(request);

        // Act and Assert - without JWT
        mockMvc.perform(patch("/api/orders/" + order.getOrderId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void updateOrderStatus_acceptsAllValidStatuses() throws Exception {
        // Arrange
        Map<String, Object> shippingAddress = new HashMap<>();
        shippingAddress.put("fullName", "John Doe");

        String[] validStatuses = {"pending", "processing", "shipped", "delivered", "cancelled"};

        for (String status : validStatuses) {
            Order order = Order.builder()
                    .userId(testUserId)
                    .orderStatus("pending")
                    .totalAmount(new BigDecimal("100.00"))
                    .shippingCost(new BigDecimal("10.00"))
                    .shippingAddress(shippingAddress)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            order = orderRepository.save(order);

            UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
            request.setStatus(status);

            String requestJson = objectMapper.writeValueAsString(request);

            // Act and Assert
            mockMvc.perform(patch("/api/orders/" + order.getOrderId() + "/status")
                            .with(jwt().jwt(jwt -> jwt.subject(testUserId)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.order_status", is(status)));
        }
    }
}
