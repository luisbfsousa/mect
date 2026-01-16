package com.shophub.service;

import com.shophub.dto.ProductDTO;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.model.Product;
import com.shophub.repository.CategoryRepository;
import com.shophub.repository.ProductRepository;
import com.shophub.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;
    
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        List<Product> products = productRepository.findAll();
        
        // Manually populate category names and review statistics
        products.forEach(product -> {
            if (product.getCategoryId() != null) {
                categoryRepository.findById(product.getCategoryId())
                    .ifPresent(category -> product.setCategoryName(category.getName()));
            }
            
            // Populate review statistics
            Double avgRating = reviewRepository.getAverageRatingByProductId(product.getProductId());
            Long reviewCount = reviewRepository.getReviewCountByProductId(product.getProductId());
            product.setAverageRating(avgRating != null ? avgRating : 0.0);
            product.setReviewCount(reviewCount != null ? reviewCount : 0L);
        });
        
        return products;
    }
    
    @Transactional(readOnly = true)
    public Product getProductById(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        
        // Populate category name
        if (product.getCategoryId() != null) {
            categoryRepository.findById(product.getCategoryId())
                .ifPresent(category -> product.setCategoryName(category.getName()));
        }
        
        // Populate review statistics
        Double avgRating = reviewRepository.getAverageRatingByProductId(product.getProductId());
        Long reviewCount = reviewRepository.getReviewCountByProductId(product.getProductId());
        product.setAverageRating(avgRating != null ? avgRating : 0.0);
        product.setReviewCount(reviewCount != null ? reviewCount : 0L);
        
        return product;
    }
    
    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(Integer categoryId) {
        List<Product> products = productRepository.findByCategoryId(categoryId);
        
        // Manually populate category names
        products.forEach(product -> {
            if (product.getCategoryId() != null) {
                categoryRepository.findById(product.getCategoryId())
                    .ifPresent(category -> product.setCategoryName(category.getName()));
            }
        });
        
        return products;
    }
    
    @Transactional
    public Product createProduct(ProductDTO productDTO) {
        // Validate category if provided
        if (productDTO.getCategoryId() != null) {
            categoryRepository.findById(productDTO.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        Integer stockQty = productDTO.getStockQuantity() != null ? productDTO.getStockQuantity() : 0;
        Integer threshold = productDTO.getLowStockThreshold() != null ? productDTO.getLowStockThreshold() : 10;

        Product product = Product.builder()
                .name(productDTO.getName())
                .description(productDTO.getDescription())
                .price(productDTO.getPrice())
                .sku(productDTO.getSku())
                .categoryId(productDTO.getCategoryId())
                .stockQuantity(stockQty)
                .lowStockThreshold(threshold)
                .images(productDTO.getImages())
                .specifications(productDTO.getSpecifications())
                .build();

        log.info("Creating new product: {}", product.getName());
        Product saved = productRepository.save(product);

        // Check for low stock alert on creation
        if (stockQty <= threshold) {
            notificationService.sendLowStockAlert(saved);
            log.info("Low stock alert sent for new product {} (stock: {}, threshold: {})",
                    saved.getProductId(), stockQty, threshold);
        }

        return saved;
    }
    
    @Transactional
    public Product updateProduct(Integer productId, ProductDTO productDTO) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Validate category if provided
        if (productDTO.getCategoryId() != null) {
            categoryRepository.findById(productDTO.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setSku(productDTO.getSku());
        product.setCategoryId(productDTO.getCategoryId());
        product.setStockQuantity(productDTO.getStockQuantity());
        product.setLowStockThreshold(productDTO.getLowStockThreshold());
        product.setImages(productDTO.getImages());
        product.setSpecifications(productDTO.getSpecifications());

        log.info("Updating product: {}", productId);
        Product saved = productRepository.save(product);

        // Check for low stock alert
        Integer threshold = saved.getLowStockThreshold() != null ? saved.getLowStockThreshold() : 10;
        Integer currentStock = saved.getStockQuantity();

        log.warn("🔍 STOCK CHECK: Product {} | Stock: {} | Threshold: {} | Condition: {}",
                productId, currentStock, threshold, (currentStock != null && currentStock <= threshold));

        if (currentStock != null && currentStock <= threshold) {
            log.warn("🚨 TRIGGERING LOW STOCK ALERT for product {}", productId);
            notificationService.sendLowStockAlert(saved);
        } else {
            log.warn("❌ NOT triggering alert. Stock: {}, Threshold: {}", currentStock, threshold);
        }

        return saved;
    }
    
    @Transactional
    public void deleteProduct(Integer productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found");
        }
        
        log.info("Deleting product: {}", productId);
        productRepository.deleteById(productId);
    }
    
    @Transactional
    public void updateStock(Integer productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        
        int newStock = product.getStockQuantity() - quantity;
        
        if (newStock < 0) {
            throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
        }
        
        product.setStockQuantity(newStock);
        log.info("Updated stock for product {}: {} -> {}", productId, product.getStockQuantity() + quantity, newStock);
        Product saved = productRepository.save(product);

        Integer threshold = saved.getLowStockThreshold() != null ? saved.getLowStockThreshold() : 10;
        log.warn("🔍 ORDER STOCK CHECK: Product {} | New Stock: {} | Threshold: {} | Condition: {}",
                productId, newStock, threshold, (newStock <= threshold));

        if (newStock <= threshold) {
            log.warn("🚨 TRIGGERING LOW STOCK ALERT from ORDER for product {}", productId);
            notificationService.sendLowStockAlert(saved);
        }
    }
    
    @Transactional
    public Product setStockQuantity(Integer productId, Integer stockQuantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        int oldStock = product.getStockQuantity();
        product.setStockQuantity(stockQuantity);

        log.info("Set stock quantity for product {}: {} -> {}", productId, oldStock, stockQuantity);
        Product saved = productRepository.save(product);

        Integer threshold = saved.getLowStockThreshold() != null ? saved.getLowStockThreshold() : 10;
        log.warn("🔍 INVENTORY UPDATE CHECK: Product {} | New Stock: {} | Threshold: {} | Condition: {}",
                productId, stockQuantity, threshold, (stockQuantity <= threshold));

        if (stockQuantity <= threshold) {
            log.warn("🚨 TRIGGERING LOW STOCK ALERT from INVENTORY UPDATE for product {}", productId);
            notificationService.sendLowStockAlert(saved);
        } else {
            log.warn("✅ Stock OK - no alert needed. Stock: {}, Threshold: {}", stockQuantity, threshold);
        }

        return saved;
    }


    
}