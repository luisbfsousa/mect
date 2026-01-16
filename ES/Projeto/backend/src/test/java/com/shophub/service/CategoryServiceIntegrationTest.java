package com.shophub.service;

import com.shophub.model.Category;
import com.shophub.repository.CategoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class CategoryServiceIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @AfterEach
    void cleanup() {
        categoryRepository.deleteAll();
    }

    @Test
    @Transactional
    void getAllCategories_returnsEmptyListWhenNoCategories() {
        // Act
        List<Category> categories = categoryService.getAllCategories();

        // Assert
        assertThat(categories).isEmpty();
    }

    @Test
    @Transactional
    void getAllCategories_returnsAllCategoriesOrderedByName() {
        // Arrange - create categories in non-alphabetical order
        categoryRepository.save(Category.builder()
                .name("Electronics")
                .description("Electronic devices and gadgets")
                .build());

        categoryRepository.save(Category.builder()
                .name("Books")
                .description("Books and magazines")
                .build());

        categoryRepository.save(Category.builder()
                .name("Clothing")
                .description("Apparel and accessories")
                .build());

        // Act
        List<Category> categories = categoryService.getAllCategories();

        // Assert
        assertThat(categories).hasSize(3);
        assertThat(categories).extracting("name")
                .containsExactly("Books", "Clothing", "Electronics"); // Alphabetical order
    }

    @Test
    @Transactional
    void createCategory_savesAndRetrievesSuccessfully() {
        // Arrange
        Category category = Category.builder()
                .name("Sports")
                .description("Sports equipment and accessories")
                .build();

        // Act
        Category saved = categoryRepository.save(category);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getCategoryId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Sports");
        assertThat(saved.getDescription()).isEqualTo("Sports equipment and accessories");
        assertThat(saved.getCreatedAt()).isNotNull();

        // Verify in database
        Optional<Category> fromDb = categoryRepository.findById(saved.getCategoryId());
        assertThat(fromDb).isPresent();
        assertThat(fromDb.get().getName()).isEqualTo("Sports");
    }

    @Test
    @Transactional
    void createCategoryWithParent_savesParentChildRelationship() {
        // Arrange - create parent category
        Category parent = Category.builder()
                .name("Electronics")
                .description("Electronic devices")
                .build();
        parent = categoryRepository.save(parent);

        // Create child category
        Category child = Category.builder()
                .name("Smartphones")
                .description("Mobile phones and accessories")
                .parentId(parent.getCategoryId())
                .build();

        // Act
        Category savedChild = categoryRepository.save(child);

        // Assert
        assertThat(savedChild.getParentId()).isEqualTo(parent.getCategoryId());

        // Verify both exist in database
        List<Category> allCategories = categoryService.getAllCategories();
        assertThat(allCategories).hasSize(2);
        assertThat(allCategories).extracting("name")
                .containsExactlyInAnyOrder("Electronics", "Smartphones");
    }

    @Test
    @Transactional
    void updateCategory_modifiesExistingCategory() {
        // Arrange
        Category category = Category.builder()
                .name("Games")
                .description("Video games")
                .build();
        category = categoryRepository.save(category);

        // Act - update the category
        category.setName("Gaming");
        category.setDescription("Video games and gaming accessories");
        Category updated = categoryRepository.save(category);

        // Assert
        assertThat(updated.getCategoryId()).isEqualTo(category.getCategoryId());
        assertThat(updated.getName()).isEqualTo("Gaming");
        assertThat(updated.getDescription()).isEqualTo("Video games and gaming accessories");

        // Verify in database
        Category fromDb = categoryRepository.findById(category.getCategoryId()).orElseThrow();
        assertThat(fromDb.getName()).isEqualTo("Gaming");
    }

    @Test
    @Transactional
    void deleteCategory_removesFromDatabase() {
        // Arrange
        Category category = Category.builder()
                .name("Toys")
                .description("Children's toys")
                .build();
        category = categoryRepository.save(category);
        Integer categoryId = category.getCategoryId();

        // Act
        categoryRepository.deleteById(categoryId);

        // Assert
        assertThat(categoryRepository.findById(categoryId)).isEmpty();
        assertThat(categoryService.getAllCategories()).isEmpty();
    }

    @Test
    @Transactional
    void findById_retrievesCategorySuccessfully() {
        // Arrange
        Category category = Category.builder()
                .name("Furniture")
                .description("Home and office furniture")
                .build();
        category = categoryRepository.save(category);

        // Act
        Optional<Category> found = categoryRepository.findById(category.getCategoryId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Furniture");
        assertThat(found.get().getDescription()).isEqualTo("Home and office furniture");
    }

    @Test
    @Transactional
    void findById_returnsEmptyForNonExistentId() {
        // Act
        Optional<Category> found = categoryRepository.findById(99999);

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @Transactional
    void createMultipleCategoriesWithSameParent_maintainsHierarchy() {
        // Arrange - create parent
        Category parent = Category.builder()
                .name("Fashion")
                .description("Fashion and apparel")
                .build();
        Category savedParent = categoryRepository.save(parent);
        final Integer parentId = savedParent.getCategoryId();

        // Create multiple children
        Category child1 = categoryRepository.save(Category.builder()
                .name("Men's Clothing")
                .description("Clothing for men")
                .parentId(parentId)
                .build());

        Category child2 = categoryRepository.save(Category.builder()
                .name("Women's Clothing")
                .description("Clothing for women")
                .parentId(parentId)
                .build());

        Category child3 = categoryRepository.save(Category.builder()
                .name("Kids' Clothing")
                .description("Clothing for children")
                .parentId(parentId)
                .build());

        // Act
        List<Category> allCategories = categoryService.getAllCategories();

        // Assert
        assertThat(allCategories).hasSize(4); // 1 parent + 3 children

        // Verify parent-child relationships
        List<Category> children = allCategories.stream()
                .filter(c -> parentId.equals(c.getParentId()))
                .toList();
        assertThat(children).hasSize(3);
        assertThat(children).extracting("name")
                .containsExactlyInAnyOrder("Men's Clothing", "Women's Clothing", "Kids' Clothing");
    }

    @Test
    @Transactional
    void categoryWithNullDescription_savesSuccessfully() {
        // Arrange
        Category category = Category.builder()
                .name("Miscellaneous")
                .description(null)
                .build();

        // Act
        Category saved = categoryRepository.save(category);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getCategoryId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Miscellaneous");
        assertThat(saved.getDescription()).isNull();
    }

    @Test
    @Transactional
    void categoryWithoutParent_savesWithNullParentId() {
        // Arrange
        Category category = Category.builder()
                .name("Root Category")
                .description("Top-level category")
                .build();

        // Act
        Category saved = categoryRepository.save(category);

        // Assert
        assertThat(saved.getParentId()).isNull();
    }
}
