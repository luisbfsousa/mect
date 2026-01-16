package com.shophub.controller;

import com.shophub.dto.BlogPostDTO;
import com.shophub.dto.CreateBlogPostRequest;
import com.shophub.dto.UpdateBlogPostRequest;
import com.shophub.service.BlogPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/blog")
@RequiredArgsConstructor
@Slf4j
//@CrossOrigin(origins = "*")
public class BlogPostController {
    
    private final BlogPostService service;
    
    // ========== PUBLIC ENDPOINTS ==========
    
    @GetMapping("/posts")
    public ResponseEntity<List<BlogPostDTO>> getPublishedPosts() {
        log.info("Fetching published posts");
        return ResponseEntity.ok(service.getPublishedPosts());
    }
    
    @GetMapping("/posts/{id}")
    public ResponseEntity<BlogPostDTO> getPost(@PathVariable Integer id) {
        log.info("Fetching post: {}", id);
        return ResponseEntity.ok(service.getPost(id));
    }
    
    // ========== ADMIN ENDPOINTS ==========
    
    @GetMapping("/admin/posts")
    @PreAuthorize("hasRole('content-manager')")
    public ResponseEntity<List<BlogPostDTO>> getAllPosts() {
        log.info("Admin fetching all posts");
        return ResponseEntity.ok(service.getAllPosts());
    }
    
    @PostMapping("/admin/posts")
    @PreAuthorize("hasRole('content-manager')")
    public ResponseEntity<BlogPostDTO> createPost(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateBlogPostRequest request) {
        
        log.info("Admin creating post: {}", request.getTitle());
        
        String authorId = jwt.getSubject();
        String authorName = jwt.getClaimAsString("name");
        
        if (authorName == null || authorName.trim().isEmpty()) {
            authorName = jwt.getClaimAsString("preferred_username");
        }
        if (authorName == null || authorName.trim().isEmpty()) {
            authorName = "Content Manager";
        }
        
        BlogPostDTO created = service.createPost(request, authorId, authorName);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/admin/posts/{id}")
    @PreAuthorize("hasRole('content-manager')")
    public ResponseEntity<BlogPostDTO> updatePost(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateBlogPostRequest request) {
        
        log.info("Admin updating post: {}", id);
        BlogPostDTO updated = service.updatePost(id, request);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/admin/posts/{id}")
    @PreAuthorize("hasRole('content-manager')")
    public ResponseEntity<Void> deletePost(@PathVariable Integer id) {
        log.info("Admin deleting post: {}", id);
        service.deletePost(id);
        return ResponseEntity.noContent().build();
    }
}