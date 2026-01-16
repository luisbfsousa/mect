package com.shophub.service;

import com.shophub.dto.BlogPostDTO;
import com.shophub.dto.CreateBlogPostRequest;
import com.shophub.dto.UpdateBlogPostRequest;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.model.BlogPost;
import com.shophub.repository.BlogPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlogPostService {
    
    private final BlogPostRepository repository;
    
    // Get all posts (for content managers)
    @Transactional(readOnly = true)
    public List<BlogPostDTO> getAllPosts() {
        return repository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    // Get published posts only (for public)
    @Transactional(readOnly = true)
    public List<BlogPostDTO> getPublishedPosts() {
        return repository.findByStatusOrderByCreatedAtDesc("published")
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    // Get single post
    @Transactional(readOnly = true)
    public BlogPostDTO getPost(Integer id) {
        BlogPost post = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
        return toDTO(post);
    }
    
    // Create post
    @Transactional
    public BlogPostDTO createPost(CreateBlogPostRequest request, String authorId, String authorName) {
        log.info("Creating post: {}", request.getTitle());
        
        BlogPost post = BlogPost.builder()
            .title(request.getTitle())
            .imageUrl(request.getImageUrl())
            .theme(request.getTheme())
            .status(request.getStatus())
            .markdownContent(request.getMarkdownContent())
            .authorId(authorId)
            .authorName(authorName)
            .build();
        
        BlogPost saved = repository.save(post);
        return toDTO(saved);
    }
    
    // Update post
    @Transactional
    public BlogPostDTO updatePost(Integer id, UpdateBlogPostRequest request) {
        log.info("Updating post: {}", id);
        
        BlogPost post = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
        
        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
        }
        if (request.getImageUrl() != null) {
            post.setImageUrl(request.getImageUrl());
        }
        if (request.getTheme() != null) {
            post.setTheme(request.getTheme());
        }
        if (request.getStatus() != null) {
            post.setStatus(request.getStatus());
        }
        if (request.getMarkdownContent() != null) {
            post.setMarkdownContent(request.getMarkdownContent());
        }
        
        BlogPost updated = repository.save(post);
        return toDTO(updated);
    }
    
    // Delete post
    @Transactional
    public void deletePost(Integer id) {
        log.info("Deleting post: {}", id);
        
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Post not found with id: " + id);
        }
        
        repository.deleteById(id);
    }
    
    // Convert entity to DTO
    private BlogPostDTO toDTO(BlogPost post) {
        return BlogPostDTO.builder()
            .id(post.getId())
            .title(post.getTitle())
            .imageUrl(post.getImageUrl())
            .theme(post.getTheme())
            .status(post.getStatus())
            .markdownContent(post.getMarkdownContent())
            .authorId(post.getAuthorId())
            .authorName(post.getAuthorName())
            .createdAt(post.getCreatedAt())
            .updatedAt(post.getUpdatedAt())
            .build();
    }
}