package com.shophub.service;

import com.shophub.dto.ProductDTO;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.model.Category;
import com.shophub.model.Product;
import com.shophub.repository.CategoryRepository;
import com.shophub.repository.ProductRepository;
import com.shophub.repository.ReviewRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
public class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @AfterEach
    void cleanup() {
        reviewRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    @Transactional
    void createProduct_savesProductSuccessfully() {
        // Arrange
        ProductDTO productDTO = ProductDTO.builder()
                .name("Test Product")
                .description("Test description")
                .price(new BigDecimal("99.99"))
                .sku("TEST-SKU-001")
                .stockQuantity(50)
                .lowStockThreshold(10)
                .images(Arrays.asList("image1.jpg", "image2.jpg"))
                .build();

        // Act
        Product created = productService.createProduct(productDTO);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getProductId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Test Product");
        assertThat(created.getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(created.getSku()).isEqualTo("TEST-SKU-001");
        assertThat(created.getStockQuantity()).isEqualTo(50);
        assertThat(created.getLowStockThreshold()).isEqualTo(10);
        assertThat(created.getImages()).hasSize(2);

        // Verify in database
        Product fromDb = productRepository.findById(created.getProductId()).orElseThrow();
        assertThat(fromDb.getName()).isEqualTo("Test Product");
    }

    @Test
    @Transactional
    void createProduct_withCategory_associatesCategoryCorrectly() {
        // Arrange - create category first
        Category category = Category.builder()
                .name("Electronics")
                .description("Electronic devices")
                .build();
        category = categoryRepository.save(category);

        ProductDTO productDTO = ProductDTO.builder()
                .name("Laptop")
                .description("High-performance laptop")
                .price(new BigDecimal("1299.99"))
                .categoryId(category.getCategoryId())
                .stockQuantity(20)
                .build();

        // Act
        Product created = productService.createProduct(productDTO);

        // Assert
        assertThat(created.getCategoryId()).isEqualTo(category.getCategoryId());

        // Verify category association
        Product fromDb = productRepository.findById(created.getProductId()).orElseThrow();
        assertThat(fromDb.getCategoryId()).isEqualTo(category.getCategoryId());
    }

    @Test
    @Transactional
    void createProduct_withInvalidCategory_throwsException() {
        // Arrange
        ProductDTO productDTO = ProductDTO.builder()
                .name("Product")
                .description("Description")
                .price(new BigDecimal("50.00"))
                .categoryId(99999) // Non-existent category
                .stockQuantity(10)
                .build();

        // Act and Assert
        assertThatThrownBy(() -> productService.createProduct(productDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    @Transactional
    void getProductById_retrievesProductWithCategoryName() {
        // Arrange
        Category category = categoryRepository.save(Category.builder()
                .name("Books")
                .build());

        Product product = productRepository.save(Product.builder()
                .name("Test Book")
                .description("A test book")
                .price(new BigDecimal("19.99"))
                .categoryId(category.getCategoryId())
                .stockQuantity(100)
                .lowStockThreshold(5)
                .build());

        // Act
        Product retrieved = productService.getProductById(product.getProductId());

        // Assert
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getProductId()).isEqualTo(product.getProductId());
        assertThat(retrieved.getName()).isEqualTo("Test Book");
        assertThat(retrieved.getCategoryName()).isEqualTo("Books");
    }

    @Test
    @Transactional
    void getProductById_throwsExceptionWhenNotFound() {
        // Act and Assert
        assertThatThrownBy(() -> productService.getProductById(99999))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @Transactional
    void getAllProducts_returnsAllProductsWithCategories() {
        // Arrange
        Category category1 = categoryRepository.save(Category.builder()
                .name("Electronics")
                .build());

        Category category2 = categoryRepository.save(Category.builder()
                .name("Clothing")
                .build());

        productRepository.save(Product.builder()
                .name("Phone")
                .description("Smartphone")
                .price(new BigDecimal("699.99"))
                .categoryId(category1.getCategoryId())
                .stockQuantity(30)
                .lowStockThreshold(5)
                .build());

        productRepository.save(Product.builder()
                .name("T-Shirt")
                .description("Cotton t-shirt")
                .price(new BigDecimal("19.99"))
                .categoryId(category2.getCategoryId())
                .stockQuantity(100)
                .lowStockThreshold(10)
                .build());

        // Act
        List<Product> products = productService.getAllProducts();

        // Assert
        assertThat(products).hasSize(2);
        assertThat(products).extracting("name")
                .containsExactlyInAnyOrder("Phone", "T-Shirt");
        assertThat(products).extracting("categoryName")
                .containsExactlyInAnyOrder("Electronics", "Clothing");
    }

    @Test
    @Transactional
    void getProductsByCategory_returnsOnlyProductsInCategory() {
        // Arrange
        Category electronics = categoryRepository.save(Category.builder()
                .name("Electronics")
                .build());

        Category books = categoryRepository.save(Category.builder()
                .name("Books")
                .build());

        productRepository.save(Product.builder()
                .name("Laptop")
                .price(new BigDecimal("999.99"))
                .categoryId(electronics.getCategoryId())
                .stockQuantity(10)
                .lowStockThreshold(2)
                .build());

        productRepository.save(Product.builder()
                .name("Phone")
                .price(new BigDecimal("699.99"))
                .categoryId(electronics.getCategoryId())
                .stockQuantity(20)
                .lowStockThreshold(5)
                .build());

        productRepository.save(Product.builder()
                .name("Novel")
                .price(new BigDecimal("14.99"))
                .categoryId(books.getCategoryId())
                .stockQuantity(50)
                .lowStockThreshold(10)
                .build());

        // Act
        List<Product> electronicsProducts = productService.getProductsByCategory(electronics.getCategoryId());

        // Assert
        assertThat(electronicsProducts).hasSize(2);
        assertThat(electronicsProducts).extracting("name")
                .containsExactlyInAnyOrder("Laptop", "Phone");
        assertThat(electronicsProducts).allMatch(p -> p.getCategoryId().equals(electronics.getCategoryId()));
    }

    @Test
    @Transactional
    void updateProduct_modifiesProductSuccessfully() {
        // Arrange
        Product product = productRepository.save(Product.builder()
                .name("Original Name")
                .description("Original description")
                .price(new BigDecimal("50.00"))
                .stockQuantity(10)
                .lowStockThreshold(2)
                .build());

        ProductDTO updateDTO = ProductDTO.builder()
                .name("Updated Name")
                .description("Updated description")
                .price(new BigDecimal("75.00"))
                .sku("UPDATED-SKU")
                .stockQuantity(20)
                .lowStockThreshold(5)
                .build();

        // Act
        Product updated = productService.updateProduct(product.getProductId(), updateDTO);

        // Assert
        assertThat(updated.getProductId()).isEqualTo(product.getProductId());
        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getDescription()).isEqualTo("Updated description");
        assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(updated.getSku()).isEqualTo("UPDATED-SKU");
        assertThat(updated.getStockQuantity()).isEqualTo(20);

        // Verify in database
        Product fromDb = productRepository.findById(product.getProductId()).orElseThrow();
        assertThat(fromDb.getName()).isEqualTo("Updated Name");
    }

    @Test
    @Transactional
    void updateProduct_withNewCategory_updatesCategoryAssociation() {
        // Arrange
        Category oldCategory = categoryRepository.save(Category.builder()
                .name("Old Category")
                .build());

        Category newCategory = categoryRepository.save(Category.builder()
                .name("New Category")
                .build());

        Product product = productRepository.save(Product.builder()
                .name("Product")
                .price(new BigDecimal("100.00"))
                .categoryId(oldCategory.getCategoryId())
                .stockQuantity(10)
                .lowStockThreshold(2)
                .build());

        ProductDTO updateDTO = ProductDTO.builder()
                .name("Product")
                .price(new BigDecimal("100.00"))
                .categoryId(newCategory.getCategoryId())
                .stockQuantity(10)
                .lowStockThreshold(2)
                .build();

        // Act
        Product updated = productService.updateProduct(product.getProductId(), updateDTO);

        // Assert
        assertThat(updated.getCategoryId()).isEqualTo(newCategory.getCategoryId());
    }

    @Test
    @Transactional
    void deleteProduct_removesProductFromDatabase() {
        // Arrange
        Product product = productRepository.save(Product.builder()
                .name("Product to Delete")
                .price(new BigDecimal("25.00"))
                .stockQuantity(5)
                .lowStockThreshold(1)
                .build());

        Integer productId = product.getProductId();

        // Act
        productService.deleteProduct(productId);

        // Assert
        assertThat(productRepository.findById(productId)).isEmpty();
    }

    @Test
    @Transactional
    void deleteProduct_throwsExceptionWhenNotFound() {
        // Act and Assert
        assertThatThrownBy(() -> productService.deleteProduct(99999))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @Transactional
    void updateStock_decreasesStockCorrectly() {
        // Arrange
        Product product = productRepository.save(Product.builder()
                .name("Stock Test Product")
                .price(new BigDecimal("30.00"))
                .stockQuantity(100)
                .lowStockThreshold(10)
                .build());

        // Act
        productService.updateStock(product.getProductId(), 25);

        // Assert
        Product updated = productRepository.findById(product.getProductId()).orElseThrow();
        assertThat(updated.getStockQuantity()).isEqualTo(75); // 100 - 25
    }

    @Test
    @Transactional
    void updateStock_throwsExceptionWhenInsufficientStock() {
        // Arrange
        Product product = productRepository.save(Product.builder()
                .name("Low Stock Product")
                .price(new BigDecimal("40.00"))
                .stockQuantity(10)
                .lowStockThreshold(2)
                .build());

        // Act and Assert
        assertThatThrownBy(() -> productService.updateStock(product.getProductId(), 15))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");

        // Verify stock unchanged
        Product unchanged = productRepository.findById(product.getProductId()).orElseThrow();
        assertThat(unchanged.getStockQuantity()).isEqualTo(10);
    }

    @Test
    @Transactional
    void createProduct_withSpecifications_savesSpecificationsCorrectly() {
        // Arrange
        Map<String, Object> specs = new HashMap<>();
        specs.put("color", "Black");
        specs.put("weight", "1.5kg");
        specs.put("dimensions", "30x20x5cm");

        ProductDTO productDTO = ProductDTO.builder()
                .name("Product with Specs")
                .description("Has specifications")
                .price(new BigDecimal("150.00"))
                .stockQuantity(15)
                .specifications(specs)
                .build();

        // Act
        Product created = productService.createProduct(productDTO);

        // Assert
        assertThat(created.getSpecifications()).isNotNull();
        assertThat(created.getSpecifications()).hasSize(3);
        assertThat(created.getSpecifications().get("color")).isEqualTo("Black");
        assertThat(created.getSpecifications().get("weight")).isEqualTo("1.5kg");
    }

    @Test
    @Transactional
    void createProduct_withDefaultValues_usesDefaults() {
        // Arrange - DTO without stock quantity and threshold
        ProductDTO productDTO = ProductDTO.builder()
                .name("Default Values Product")
                .description("Uses default stock values")
                .price(new BigDecimal("25.00"))
                .build();

        // Act
        Product created = productService.createProduct(productDTO);

        // Assert
        assertThat(created.getStockQuantity()).isEqualTo(0); // Default stock
        assertThat(created.getLowStockThreshold()).isEqualTo(10); // Default threshold
    }
}
