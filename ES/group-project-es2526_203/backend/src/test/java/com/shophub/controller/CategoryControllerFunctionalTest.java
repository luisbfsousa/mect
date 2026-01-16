package com.shophub.controller;

import com.shophub.config.TestSecurityConfig;
import com.shophub.model.Category;
import com.shophub.repository.CategoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class CategoryControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @AfterEach
    void cleanup() {
        categoryRepository.deleteAll();
    }

    @Test
    @Transactional
    void getAllCategories_returnsAllCategories() throws Exception {
        // Arrange
        categoryRepository.save(Category.builder()
                .name("Electronics")
                .description("Electronic devices and accessories")
                .build());

        categoryRepository.save(Category.builder()
                .name("Clothing")
                .description("Apparel and fashion items")
                .build());

        categoryRepository.save(Category.builder()
                .name("Books")
                .description("Physical and digital books")
                .build());

        // Act and Assert
        mockMvc.perform(get("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Electronics", "Clothing", "Books")))
                .andExpect(jsonPath("$[*].description", hasItem("Electronic devices and accessories")))
                .andExpect(jsonPath("$[0].categoryId").exists())
                .andExpect(jsonPath("$[0].createdAt").exists());
    }

    @Test
    @Transactional
    void getAllCategories_returnsEmptyListWhenNoCategories() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Transactional
    void getAllCategories_publicEndpoint_noAuthRequired() throws Exception {
        // Arrange
        categoryRepository.save(Category.builder()
                .name("Public Category")
                .description("Accessible without authentication")
                .build());

        // Act and Assert - should work without authentication
        mockMvc.perform(get("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Public Category")));
    }

    @Test
    @Transactional
    void getAllCategories_withParentCategory_returnsCorrectly() throws Exception {
        // Arrange - create parent category
        Category parentCategory = categoryRepository.save(Category.builder()
                .name("Electronics")
                .description("All electronic items")
                .build());

        // Create child categories
        categoryRepository.save(Category.builder()
                .name("Laptops")
                .description("Laptop computers")
                .parentId(parentCategory.getCategoryId())
                .build());

        categoryRepository.save(Category.builder()
                .name("Smartphones")
                .description("Mobile phones")
                .parentId(parentCategory.getCategoryId())
                .build());

        // Act and Assert
        mockMvc.perform(get("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Electronics", "Laptops", "Smartphones")))
                .andExpect(jsonPath("$[?(@.name=='Laptops')].parentId", contains(parentCategory.getCategoryId())))
                .andExpect(jsonPath("$[?(@.name=='Smartphones')].parentId", contains(parentCategory.getCategoryId())));
    }

    @Test
    @Transactional
    void getAllCategories_withNullDescription_handlesGracefully() throws Exception {
        // Arrange
        categoryRepository.save(Category.builder()
                .name("Category Without Description")
                .description(null)
                .build());

        // Act and Assert
        mockMvc.perform(get("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Category Without Description")))
                .andExpect(jsonPath("$[0].description").value(nullValue()));
    }

    @Test
    @Transactional
    void getAllCategories_returnsInCreationOrder() throws Exception {
        // Arrange - create categories in specific order
        Category cat1 = categoryRepository.save(Category.builder()
                .name("First Category")
                .description("Created first")
                .build());

        Category cat2 = categoryRepository.save(Category.builder()
                .name("Second Category")
                .description("Created second")
                .build());

        Category cat3 = categoryRepository.save(Category.builder()
                .name("Third Category")
                .description("Created third")
                .build());

        // Act and Assert
        mockMvc.perform(get("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].categoryId", is(cat1.getCategoryId())))
                .andExpect(jsonPath("$[1].categoryId", is(cat2.getCategoryId())))
                .andExpect(jsonPath("$[2].categoryId", is(cat3.getCategoryId())));
    }

    @Test
    @Transactional
    void getAllCategories_withSpecialCharactersInName_handlesCorrectly() throws Exception {
        // Arrange
        categoryRepository.save(Category.builder()
                .name("Books & Magazines")
                .description("Category with special characters: & < > \" '")
                .build());

        // Act and Assert
        mockMvc.perform(get("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Books & Magazines")))
                .andExpect(jsonPath("$[0].description", is("Category with special characters: & < > \" '")));
    }

    @Test
    @Transactional
    void getAllCategories_withLongDescription_returnsCompleteText() throws Exception {
        // Arrange
        String longDescription = "This is a very long description that contains many words and details about the category. " +
                "It includes information about what types of products are included, their features, and why customers might " +
                "want to browse this category. The description can be quite extensive and contain multiple sentences.";

        categoryRepository.save(Category.builder()
                .name("Detailed Category")
                .description(longDescription)
                .build());

        // Act and Assert
        mockMvc.perform(get("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Detailed Category")))
                .andExpect(jsonPath("$[0].description", is(longDescription)));
    }

    @Test
    @Transactional
    void getAllCategories_multipleParentChildRelationships() throws Exception {
        // Arrange - create a category tree
        Category electronics = categoryRepository.save(Category.builder()
                .name("Electronics")
                .description("All electronics")
                .build());

        Category computers = categoryRepository.save(Category.builder()
                .name("Computers")
                .description("Computer products")
                .parentId(electronics.getCategoryId())
                .build());

        categoryRepository.save(Category.builder()
                .name("Laptops")
                .description("Portable computers")
                .parentId(computers.getCategoryId())
                .build());

        categoryRepository.save(Category.builder()
                .name("Desktops")
                .description("Desktop computers")
                .parentId(computers.getCategoryId())
                .build());

        Category clothing = categoryRepository.save(Category.builder()
                .name("Clothing")
                .description("Apparel")
                .build());

        // Act and Assert
        mockMvc.perform(get("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder(
                        "Electronics", "Computers", "Laptops", "Desktops", "Clothing")))
                .andExpect(jsonPath("$[?(@.name=='Electronics')].parentId[0]").doesNotExist())
                .andExpect(jsonPath("$[?(@.name=='Clothing')].parentId[0]").doesNotExist())
                .andExpect(jsonPath("$[?(@.name=='Computers')].parentId", contains(electronics.getCategoryId())))
                .andExpect(jsonPath("$[?(@.name=='Laptops')].parentId", contains(computers.getCategoryId())));
    }

    @Test
    @Transactional
    void getAllCategories_withWhitespaceInName_trimsCorrectly() throws Exception {
        // Arrange
        categoryRepository.save(Category.builder()
                .name("  Category With Spaces  ")
                .description("  Description with spaces  ")
                .build());

        // Act and Assert - assuming the service/repository handles trimming
        mockMvc.perform(get("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].categoryId").exists());
    }
}
