package com.shophub.controller;

import com.shophub.dto.ProductDTO;
import com.shophub.model.Order;
import com.shophub.model.Product;
import com.shophub.service.OrderService;
import com.shophub.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private AdminController controller;

    private Product sampleProduct;
    private ProductDTO productDTO;
    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        // Sample product
        sampleProduct = Product.builder()
                .productId(1)
                .name("Laptop")
                .description("Gaming laptop")
                .price(new BigDecimal("1299.99"))
                .stockQuantity(10)
                .categoryId(1)
                .categoryName("Electronics")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Product DTO
        productDTO = ProductDTO.builder()
                .name("New Laptop")
                .description("High-end gaming laptop")
                .price(new BigDecimal("1499.99"))
                .categoryId(1)
                .stockQuantity(5)
                .lowStockThreshold(2)
                .build();

        // Sample order
        sampleOrder = Order.builder()
                .orderId(1)
                .userId("user-123")
                .orderStatus("pending")
                .totalAmount(new BigDecimal("199.99"))
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Test: Admin gets all products
     */
    @Test
    void getAllProducts_ShouldReturnAllProducts() {
        // Given
        List<Product> products = Arrays.asList(sampleProduct);
        when(productService.getAllProducts()).thenReturn(products);

        // When
        ResponseEntity<List<Product>> response = controller.getAllProducts();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Laptop", response.getBody().get(0).getName());
        
        verify(productService, times(1)).getAllProducts();
    }

    /**
     * Test: Admin creates a new product
     */
    @Test
    void createProduct_ShouldReturnCreatedProduct_WhenValidData() {
        // Given
        when(productService.createProduct(any(ProductDTO.class))).thenReturn(sampleProduct);

        // When
        ResponseEntity<Product> response = controller.createProduct(productDTO);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Laptop", response.getBody().getName());
        
        verify(productService, times(1)).createProduct(any(ProductDTO.class));
    }

    /**
     * Test: Admin updates an existing product
     */
    @Test
    void updateProduct_ShouldReturnUpdatedProduct_WhenValidData() {
        // Given
        Product updatedProduct = Product.builder()
                .productId(1)
                .name("Updated Laptop")
                .price(new BigDecimal("1599.99"))
                .stockQuantity(15)
                .build();
        
        when(productService.updateProduct(eq(1), any(ProductDTO.class))).thenReturn(updatedProduct);

        // When
        ResponseEntity<Product> response = controller.updateProduct(1, productDTO);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Updated Laptop", response.getBody().getName());
        assertEquals(new BigDecimal("1599.99"), response.getBody().getPrice());
        
        verify(productService, times(1)).updateProduct(eq(1), any(ProductDTO.class));
    }

    /**
     * Test: Admin deletes a product
     */
    @Test
    void deleteProduct_ShouldReturnSuccessMessage_WhenProductDeleted() {
        // Given
        doNothing().when(productService).deleteProduct(1);

        // When
        ResponseEntity<Map<String, String>> response = controller.deleteProduct(1);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Product deleted successfully", response.getBody().get("message"));
        
        verify(productService, times(1)).deleteProduct(1);
    }

    /**
     * Test: Admin gets all orders
     */
    @Test
    void getAllOrders_ShouldReturnAllOrders_WhenNoFilters() {
        // Given
        List<Order> orders = Arrays.asList(sampleOrder);
        when(orderService.getAllOrders(null, null)).thenReturn(orders);

        // When
        ResponseEntity<List<Order>> response = controller.getAllOrders(null, null);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("pending", response.getBody().get(0).getOrderStatus());
        
        verify(orderService, times(1)).getAllOrders(null, null);
    }

    /**
     * Test: Admin gets orders filtered by status
     */
    @Test
    void getAllOrders_ShouldReturnFilteredOrders_WhenStatusProvided() {
        // Given
        List<Order> pendingOrders = Arrays.asList(sampleOrder);
        when(orderService.getAllOrders("pending", null)).thenReturn(pendingOrders);

        // When
        ResponseEntity<List<Order>> response = controller.getAllOrders("pending", null);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("pending", response.getBody().get(0).getOrderStatus());
        
        verify(orderService, times(1)).getAllOrders("pending", null);
    }

    /**
     * Test: Admin gets orders with search
     */
    @Test
    void getAllOrders_ShouldReturnSearchResults_WhenSearchProvided() {
        // Given
        when(orderService.getAllOrders(null, "user-123")).thenReturn(Arrays.asList(sampleOrder));

        // When
        ResponseEntity<List<Order>> response = controller.getAllOrders(null, "user-123");

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        
        verify(orderService, times(1)).getAllOrders(null, "user-123");
    }

    /**
     * Test: Admin gets order statistics
     */
    @Test
    void getOrderStatistics_ShouldReturnStatsMap() {
        // When
        ResponseEntity<Map<String, Object>> response = controller.getOrderStatistics();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
    }
}
