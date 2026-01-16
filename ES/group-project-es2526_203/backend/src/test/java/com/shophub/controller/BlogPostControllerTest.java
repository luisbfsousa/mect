package com.shophub.controller;

import com.shophub.dto.BlogPostDTO;
import com.shophub.dto.CreateBlogPostRequest;
import com.shophub.dto.UpdateBlogPostRequest;
import com.shophub.service.BlogPostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlogPostControllerTest {

    @Mock
    private BlogPostService service;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private BlogPostController controller;

    private BlogPostDTO samplePost;
    private CreateBlogPostRequest createRequest;
    private UpdateBlogPostRequest updateRequest;

    @BeforeEach
    void setUp() {
        // Sample blog post
        samplePost = BlogPostDTO.builder()
                .id(1)
                .title("Getting Started with Spring Boot")
                .imageUrl("https://example.com/image.jpg")
                .theme("Technology")
                .status("published")
                .markdownContent("# Introduction\nThis is a blog post about Spring Boot.")
                .authorId("author-123")
                .authorName("John Doe")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Create request
        createRequest = CreateBlogPostRequest.builder()
                .title("New Blog Post")
                .imageUrl("https://example.com/new.jpg")
                .theme("Technology")
                .status("draft")
                .markdownContent("# New Post\nContent here.")
                .build();

        // Update request
        updateRequest = UpdateBlogPostRequest.builder()
                .title("Updated Title")
                .status("published")
                .build();
    }

    /**
     * Test: Create a new blog post
     */
    @Test
    void createPost_ShouldReturnCreatedPost_WhenValidRequest() {
        // Given
        when(jwt.getSubject()).thenReturn("author-123");
        when(jwt.getClaimAsString("name")).thenReturn("John Doe");
        when(service.createPost(any(CreateBlogPostRequest.class), anyString(), anyString()))
                .thenReturn(samplePost);

        // When
        ResponseEntity<BlogPostDTO> response = controller.createPost(jwt, createRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Getting Started with Spring Boot", response.getBody().getTitle());
        
        verify(service, times(1)).createPost(any(CreateBlogPostRequest.class), eq("author-123"), eq("John Doe"));
    }

    /**
     * Test: Get all published blog posts
     */
    @Test
    void getPublishedPosts_ShouldReturnPublishedPosts() {
        // Given
        List<BlogPostDTO> posts = Arrays.asList(samplePost);
        when(service.getPublishedPosts()).thenReturn(posts);

        // When
        ResponseEntity<List<BlogPostDTO>> response = controller.getPublishedPosts();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("published", response.getBody().get(0).getStatus());
        
        verify(service, times(1)).getPublishedPosts();
    }

    /**
     * Test: Get a specific blog post by ID
     */
    @Test
    void getPost_ShouldReturnPost_WhenPostExists() {
        // Given
        when(service.getPost(1)).thenReturn(samplePost);

        // When
        ResponseEntity<BlogPostDTO> response = controller.getPost(1);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getId());
        assertEquals("Getting Started with Spring Boot", response.getBody().getTitle());
        
        verify(service, times(1)).getPost(1);
    }

    /**
     * Test: Get all blog posts
     */
    @Test
    void getAllPosts_ShouldReturnAllPosts_IncludingDrafts() {
        // Given
        BlogPostDTO draftPost = BlogPostDTO.builder()
                .id(2)
                .title("Draft Post")
                .status("draft")
                .build();
        
        List<BlogPostDTO> posts = Arrays.asList(samplePost, draftPost);
        when(service.getAllPosts()).thenReturn(posts);

        // When
        ResponseEntity<List<BlogPostDTO>> response = controller.getAllPosts();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        
        verify(service, times(1)).getAllPosts();
    }

    /**
     * Test: Update an existing blog post
     */
    @Test
    void updatePost_ShouldReturnUpdatedPost_WhenPostExists() {
        // Given
        BlogPostDTO updatedPost = BlogPostDTO.builder()
                .id(1)
                .title("Updated Title")
                .status("published")
                .build();
        
        when(service.updatePost(eq(1), any(UpdateBlogPostRequest.class)))
                .thenReturn(updatedPost);

        // When
        ResponseEntity<BlogPostDTO> response = controller.updatePost(1, updateRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Updated Title", response.getBody().getTitle());
        assertEquals("published", response.getBody().getStatus());
        
        verify(service, times(1)).updatePost(eq(1), any(UpdateBlogPostRequest.class));
    }

    /**
     * Test: Delete a blog post
     */
    @Test
    void deletePost_ShouldReturnNoContent_WhenPostDeleted() {
        // Given
        doNothing().when(service).deletePost(1);

        // When
        ResponseEntity<Void> response = controller.deletePost(1);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        
        verify(service, times(1)).deletePost(1);
    }
}
