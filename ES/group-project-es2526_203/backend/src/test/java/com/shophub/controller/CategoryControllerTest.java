package com.shophub.controller;

import com.shophub.model.Category;
import com.shophub.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController controller;

    private Category category1;
    private Category category2;

    @BeforeEach
    void setUp() {
        category1 = Category.builder()
                .categoryId(1)
                .name("Electronics")
                .description("Electronic devices and gadgets")
                .createdAt(LocalDateTime.now())
                .build();

        category2 = Category.builder()
                .categoryId(2)
                .name("Books")
                .description("Books and literature")
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Test: Get all categories
     */
    @Test
    void getAllCategories_ShouldReturnCategoryList_WhenCategoriesExist() {
        // Given
        List<Category> categories = Arrays.asList(category1, category2);
        when(categoryService.getAllCategories()).thenReturn(categories);

        // When
        ResponseEntity<List<Category>> response = controller.getAllCategories();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("Electronics", response.getBody().get(0).getName());
        assertEquals("Books", response.getBody().get(1).getName());
        
        verify(categoryService, times(1)).getAllCategories();
    }

    /**
     * Test: Verify categories have required fields
     */
    @Test
    void getAllCategories_ShouldReturnCategoriesWithRequiredFields() {
        // Given
        List<Category> categories = Arrays.asList(category1, category2);
        when(categoryService.getAllCategories()).thenReturn(categories);

        // When
        ResponseEntity<List<Category>> response = controller.getAllCategories();

        // Then
        assertNotNull(response.getBody());
        response.getBody().forEach(category -> {
            assertNotNull(category.getCategoryId(), "Category ID should not be null");
            assertNotNull(category.getName(), "Category name should not be null");
            assertFalse(category.getName().isEmpty(), "Category name should not be empty");
        });
        
        verify(categoryService, times(1)).getAllCategories();
    }

    /**
     * Test: Categories are returned in the order provided by service
     */
    @Test
    void getAllCategories_ShouldPreserveOrderFromService() {
        // Given
        Category cat1 = Category.builder().categoryId(3).name("Clothing").build();
        Category cat2 = Category.builder().categoryId(1).name("Electronics").build();
        Category cat3 = Category.builder().categoryId(2).name("Books").build();
        
        List<Category> orderedCategories = Arrays.asList(cat1, cat2, cat3);
        when(categoryService.getAllCategories()).thenReturn(orderedCategories);

        // When
        ResponseEntity<List<Category>> response = controller.getAllCategories();

        // Then
        assertEquals(3, response.getBody().size());
        assertEquals("Clothing", response.getBody().get(0).getName());
        assertEquals("Electronics", response.getBody().get(1).getName());
        assertEquals("Books", response.getBody().get(2).getName());
        
        verify(categoryService, times(1)).getAllCategories();
    }

    /**
     * Test: Single category in list
     */
    @Test
    void getAllCategories_ShouldReturnSingleCategory_WhenOnlyOneExists() {
        // Given
        when(categoryService.getAllCategories()).thenReturn(Arrays.asList(category1));

        // When
        ResponseEntity<List<Category>> response = controller.getAllCategories();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Electronics", response.getBody().get(0).getName());
        
        verify(categoryService, times(1)).getAllCategories();
    }
}
