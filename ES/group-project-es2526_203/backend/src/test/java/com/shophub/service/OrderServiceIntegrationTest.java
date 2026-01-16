package com.shophub.service;

import com.shophub.dto.CreateOrderRequest;
import com.shophub.model.Order;
import com.shophub.model.Product;
import com.shophub.repository.OrderItemRepository;
import com.shophub.repository.OrderRepository;
import com.shophub.repository.ProductRepository;
import com.shophub.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanup() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    void createOrder_endToEnd_createsOrderAndReducesStock() {
        // Arrange - create a product with stock
        Product product = Product.builder()
                .name("Integration Product")
                .description("Test product for integration")
                .price(new BigDecimal("10.00"))
                .stockQuantity(10)
                .lowStockThreshold(1)
                .build();

        product = productRepository.save(product);

        // Build order request
        CreateOrderRequest.OrderItemRequest itemReq = new CreateOrderRequest.OrderItemRequest();
        itemReq.setProductId(product.getProductId());
        itemReq.setQuantity(2);
        itemReq.setPrice(product.getPrice());

        CreateOrderRequest.Address address = new CreateOrderRequest.Address(
                "Test User", "123 Street", "City", "0000", "+100000000"
        );

        CreateOrderRequest.ShippingInfo shipping = new CreateOrderRequest.ShippingInfo(address, new BigDecimal("2.00"));

        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(List.of(itemReq))
                .total(new BigDecimal("22.00"))
                .shipping(shipping)
                .build();

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "itest-user-1")
                .claim("email", "itest@example.com")
                .claim("given_name", "ITest")
                .claim("family_name", "User")
                .claim("name", "ITest User")
                .build();

        String userId = jwt.getSubject();

        // Act
        Order created = orderService.createOrder(userId, request, jwt);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getOrderId()).isNotNull();
        assertThat(created.getItems()).isNotNull();
        assertThat(created.getItems()).hasSize(1);
        assertThat(created.getOrderStatus()).isEqualTo("pending");

        // verify product stock decreased
        Product after = productRepository.findById(product.getProductId()).orElseThrow();
        assertThat(after.getStockQuantity()).isEqualTo(8);

        // verify user was created
        assertThat(userRepository.existsById(userId)).isTrue();

        // verify getUserOrders returns the created order
        var userOrders = orderService.getUserOrders(userId);
        assertThat(userOrders).isNotEmpty();
        assertThat(userOrders.get(0).getOrderId()).isEqualTo(created.getOrderId());
    }
}
