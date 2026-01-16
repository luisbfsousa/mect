package com.shophub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shophub.config.TestSecurityConfig;
import com.shophub.dto.CreateBlogPostRequest;
import com.shophub.dto.UpdateBlogPostRequest;
import com.shophub.model.BlogPost;
import com.shophub.repository.BlogPostRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class BlogPostControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BlogPostRepository blogPostRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void cleanup() {
        blogPostRepository.deleteAll();
    }

    @Test
    @Transactional
    void getPublishedPosts_returnsOnlyPublishedPosts() throws Exception {
        // Arrange
        blogPostRepository.save(BlogPost.builder()
                .title("Published Post 1")
                .status("published")
                .markdownContent("# Content 1")
                .authorId("author-1")
                .authorName("Author One")
                .build());

        blogPostRepository.save(BlogPost.builder()
                .title("Draft Post")
                .status("draft")
                .markdownContent("# Draft Content")
                .authorId("author-2")
                .authorName("Author Two")
                .build());

        blogPostRepository.save(BlogPost.builder()
                .title("Published Post 2")
                .status("published")
                .markdownContent("# Content 2")
                .authorId("author-1")
                .authorName("Author One")
                .build());

        // Act and Assert
        mockMvc.perform(get("/api/blog/posts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].status", everyItem(is("published"))))
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("Published Post 1", "Published Post 2")));
    }

    @Test
    @Transactional
    void getPublishedPosts_returnsEmptyListWhenNoPosts() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/blog/posts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Transactional
    void getPostById_returnsPublishedPost() throws Exception {
        // Arrange
        BlogPost post = blogPostRepository.save(BlogPost.builder()
                .title("Test Post")
                .imageUrl("https://example.com/image.jpg")
                .theme("Technology")
                .status("published")
                .markdownContent("# Test Content")
                .authorId("author-123")
                .authorName("John Doe")
                .build());

        // Act and Assert
        mockMvc.perform(get("/api/blog/posts/{id}", post.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(post.getId())))
                .andExpect(jsonPath("$.title", is("Test Post")))
                .andExpect(jsonPath("$.image_url", is("https://example.com/image.jpg")))
                .andExpect(jsonPath("$.theme", is("Technology")))
                .andExpect(jsonPath("$.status", is("published")))
                .andExpect(jsonPath("$.markdown_content", is("# Test Content")))
                .andExpect(jsonPath("$.author_id", is("author-123")))
                .andExpect(jsonPath("$.author_name", is("John Doe")));
    }

    @Test
    @Transactional
    void getPostById_returns404WhenNotFound() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/blog/posts/{id}", 99999)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    @WithMockUser(roles = "content-manager")
    void getAllPosts_asContentManager_returnsAllPosts() throws Exception {
        // Arrange
        blogPostRepository.save(BlogPost.builder()
                .title("Published Post")
                .status("published")
                .markdownContent("# Published")
                .authorId("author-1")
                .authorName("Author One")
                .build());

        blogPostRepository.save(BlogPost.builder()
                .title("Draft Post")
                .status("draft")
                .markdownContent("# Draft")
                .authorId("author-2")
                .authorName("Author Two")
                .build());

        // Act and Assert
        mockMvc.perform(get("/api/blog/admin/posts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].status", containsInAnyOrder("published", "draft")));
    }

    @Test
    @Transactional
    @WithMockUser(roles = "customer")
    void getAllPosts_asCustomer_returns403() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/blog/admin/posts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void getAllPosts_withoutAuth_returns403() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/api/blog/admin/posts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but got: " + status);
                    }
                });
    }

    @Test
    @Transactional
    void createPost_asContentManager_createsAndReturns201() throws Exception {
        // Arrange
        CreateBlogPostRequest request = CreateBlogPostRequest.builder()
                .title("New Blog Post")
                .imageUrl("https://example.com/new-image.jpg")
                .theme("Technology")
                .status("draft")
                .markdownContent("# New Post Content\n\nThis is a test post.")
                .build();

        // Act and Assert
        mockMvc.perform(post("/api/blog/admin/posts").with(csrf())
                        .with(jwt()
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_content-manager"))
                                .jwt(jwt -> jwt
                                        .subject("test-user-123")
                                        .claim("name", "Test User")
                                        .claim("preferred_username", "testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title", is("New Blog Post")))
                .andExpect(jsonPath("$.image_url", is("https://example.com/new-image.jpg")))
                .andExpect(jsonPath("$.theme", is("Technology")))
                .andExpect(jsonPath("$.status", is("draft")))
                .andExpect(jsonPath("$.markdown_content", is("# New Post Content\n\nThis is a test post.")))
                .andExpect(jsonPath("$.author_id", is("test-user-123")))
                .andExpect(jsonPath("$.author_name", is("Test User")))
                .andExpect(jsonPath("$.created_at").exists())
                .andExpect(jsonPath("$.updated_at").exists());
    }

    @Test
    @Transactional
    @WithMockUser(roles = "content-manager")
    void createPost_withInvalidData_returns400() throws Exception {
        // Arrange - missing required fields
        CreateBlogPostRequest request = CreateBlogPostRequest.builder()
                .imageUrl("https://example.com/image.jpg")
                // Missing title, status, and markdownContent
                .build();

        // Act and Assert
        mockMvc.perform(post("/api/blog/admin/posts").with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @WithMockUser(roles = "content-manager")
    void updatePost_asContentManager_updatesAndReturnsPost() throws Exception {
        // Arrange - create existing post
        BlogPost existingPost = blogPostRepository.save(BlogPost.builder()
                .title("Old Title")
                .imageUrl("https://example.com/old-image.jpg")
                .theme("Old Theme")
                .status("draft")
                .markdownContent("# Old Content")
                .authorId("author-123")
                .authorName("Original Author")
                .build());

        UpdateBlogPostRequest updateRequest = UpdateBlogPostRequest.builder()
                .title("Updated Title")
                .imageUrl("https://example.com/new-image.jpg")
                .theme("New Theme")
                .status("published")
                .markdownContent("# Updated Content")
                .build();

        // Act and Assert
        mockMvc.perform(put("/api/blog/admin/posts/{id}", existingPost.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(existingPost.getId())))
                .andExpect(jsonPath("$.title", is("Updated Title")))
                .andExpect(jsonPath("$.image_url", is("https://example.com/new-image.jpg")))
                .andExpect(jsonPath("$.theme", is("New Theme")))
                .andExpect(jsonPath("$.status", is("published")))
                .andExpect(jsonPath("$.markdown_content", is("# Updated Content")))
                .andExpect(jsonPath("$.author_id", is("author-123"))) // Author ID should not change
                .andExpect(jsonPath("$.author_name", is("Original Author"))); // Author name should not change
    }

    @Test
    @Transactional
    @WithMockUser(roles = "content-manager")
    void updatePost_nonExistent_returns404() throws Exception {
        // Arrange
        UpdateBlogPostRequest updateRequest = UpdateBlogPostRequest.builder()
                .title("Updated Title")
                .status("published")
                .markdownContent("# Updated Content")
                .build();

        // Act and Assert
        mockMvc.perform(put("/api/blog/admin/posts/{id}", 99999).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    @WithMockUser(roles = "content-manager")
    void deletePost_asContentManager_deletesAndReturns204() throws Exception {
        // Arrange
        BlogPost post = blogPostRepository.save(BlogPost.builder()
                .title("Post to Delete")
                .status("draft")
                .markdownContent("# Content to delete")
                .authorId("author-123")
                .authorName("Author")
                .build());

        // Act and Assert
        mockMvc.perform(delete("/api/blog/admin/posts/{id}", post.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Verify post is actually deleted
        mockMvc.perform(get("/api/blog/posts/{id}", post.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    @WithMockUser(roles = "content-manager")
    void deletePost_nonExistent_returns404() throws Exception {
        // Act and Assert
        mockMvc.perform(delete("/api/blog/admin/posts/{id}", 99999).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    @WithMockUser(roles = "customer")
    void createPost_asCustomer_returns403() throws Exception {
        // Arrange
        CreateBlogPostRequest request = CreateBlogPostRequest.builder()
                .title("Unauthorized Post")
                .status("draft")
                .markdownContent("# Content")
                .build();

        // Act and Assert
        mockMvc.perform(post("/api/blog/admin/posts").with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @WithMockUser(roles = "customer")
    void updatePost_asCustomer_returns403() throws Exception {
        // Arrange
        BlogPost post = blogPostRepository.save(BlogPost.builder()
                .title("Test Post")
                .status("published")
                .markdownContent("# Content")
                .authorId("author-123")
                .authorName("Author")
                .build());

        UpdateBlogPostRequest updateRequest = UpdateBlogPostRequest.builder()
                .title("Unauthorized Update")
                .build();

        // Act and Assert
        mockMvc.perform(put("/api/blog/admin/posts/{id}", post.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @WithMockUser(roles = "customer")
    void deletePost_asCustomer_returns403() throws Exception {
        // Arrange
        BlogPost post = blogPostRepository.save(BlogPost.builder()
                .title("Test Post")
                .status("published")
                .markdownContent("# Content")
                .authorId("author-123")
                .authorName("Author")
                .build());

        // Act and Assert
        mockMvc.perform(delete("/api/blog/admin/posts/{id}", post.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void getPublishedPosts_publicEndpoint_noAuthRequired() throws Exception {
        // Arrange
        blogPostRepository.save(BlogPost.builder()
                .title("Public Post")
                .status("published")
                .markdownContent("# Public Content")
                .authorId("author-1")
                .authorName("Author One")
                .build());

        // Act and Assert - should work without authentication
        mockMvc.perform(get("/api/blog/posts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Public Post")));
    }

    @Test
    @Transactional
    void getPostById_publicEndpoint_noAuthRequired() throws Exception {
        // Arrange
        BlogPost post = blogPostRepository.save(BlogPost.builder()
                .title("Public Post")
                .status("published")
                .markdownContent("# Public Content")
                .authorId("author-1")
                .authorName("Author One")
                .build());

        // Act and Assert - should work without authentication
        mockMvc.perform(get("/api/blog/posts/{id}", post.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(post.getId())))
                .andExpect(jsonPath("$.title", is("Public Post")));
    }
}
