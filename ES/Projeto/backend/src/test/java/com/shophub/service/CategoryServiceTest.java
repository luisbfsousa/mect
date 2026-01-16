package com.shophub.service;

import com.shophub.model.Category;
import com.shophub.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category testCategory1;
    private Category testCategory2;

    @BeforeEach
    void setUp() {
        testCategory1 = Category.builder()
                .categoryId(1)
                .name("Electronics")
                .description("Electronic devices and gadgets")
                .build();

        testCategory2 = Category.builder()
                .categoryId(2)
                .name("Clothing")
                .description("Apparel and fashion items")
                .build();
    }

    @Test
    void getAllCategories_ShouldReturnAllCategoriesOrderedByName() {
        // Given - Repository returns sorted list: Clothing, Electronics (alphabetical order)
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(Arrays.asList(testCategory2, testCategory1));

        // When
        List<Category> result = categoryService.getAllCategories();

        // Then
        assertEquals(2, result.size());
        assertEquals("Clothing", result.get(0).getName()); // Should be sorted by name
        assertEquals("Electronics", result.get(1).getName());
        verify(categoryRepository).findAllByOrderByNameAsc();
    }

    @Test
    void getAllCategories_ShouldReturnEmptyListWhenNoCategories() {
        // Given
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(Arrays.asList());

        // When
        List<Category> result = categoryService.getAllCategories();

        // Then
        assertTrue(result.isEmpty());
        verify(categoryRepository).findAllByOrderByNameAsc();
    }
}
