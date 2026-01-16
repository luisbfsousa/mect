package com.shophub.service;

import com.shophub.dto.BlogPostDTO;
import com.shophub.dto.CreateBlogPostRequest;
import com.shophub.dto.UpdateBlogPostRequest;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.model.BlogPost;
import com.shophub.repository.BlogPostRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
public class BlogPostServiceIntegrationTest {

    @Autowired
    private BlogPostService blogPostService;

    @Autowired
    private BlogPostRepository blogPostRepository;

    @AfterEach
    void cleanup() {
        blogPostRepository.deleteAll();
    }

    @Test
    @Transactional
    void createPost_savesPostSuccessfully() {
        // Arrange
        CreateBlogPostRequest request = CreateBlogPostRequest.builder()
                .title("Integration Test Post")
                .imageUrl("https://example.com/image.jpg")
                .theme("Technology")
                .status("draft")
                .markdownContent("# Test Content\n\nThis is a test blog post.")
                .build();

        String authorId = "author-123";
        String authorName = "Test Author";

        // Act
        BlogPostDTO created = blogPostService.createPost(request, authorId, authorName);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getTitle()).isEqualTo("Integration Test Post");
        assertThat(created.getAuthorId()).isEqualTo(authorId);
        assertThat(created.getAuthorName()).isEqualTo(authorName);
        assertThat(created.getStatus()).isEqualTo("draft");
        assertThat(created.getTheme()).isEqualTo("Technology");
        assertThat(created.getCreatedAt()).isNotNull();

        // Verify in database
        BlogPost fromDb = blogPostRepository.findById(created.getId()).orElseThrow();
        assertThat(fromDb.getTitle()).isEqualTo("Integration Test Post");
        assertThat(fromDb.getMarkdownContent()).contains("Test Content");
    }

    @Test
    @Transactional
    void updatePost_updatesFieldsSuccessfully() {
        // Arrange - create a post first
        BlogPost post = BlogPost.builder()
                .title("Original Title")
                .imageUrl("https://example.com/old.jpg")
                .theme("Old Theme")
                .status("draft")
                .markdownContent("Original content")
                .authorId("author-1")
                .authorName("Author One")
                .build();
        post = blogPostRepository.save(post);

        // Create update request
        UpdateBlogPostRequest updateRequest = UpdateBlogPostRequest.builder()
                .title("Updated Title")
                .status("published")
                .theme("Updated Theme")
                .build();

        // Act
        BlogPostDTO updated = blogPostService.updatePost(post.getId(), updateRequest);

        // Assert
        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getStatus()).isEqualTo("published");
        assertThat(updated.getTheme()).isEqualTo("Updated Theme");
        // Original fields should remain unchanged
        assertThat(updated.getAuthorId()).isEqualTo("author-1");
        assertThat(updated.getMarkdownContent()).isEqualTo("Original content");

        // Verify in database
        BlogPost fromDb = blogPostRepository.findById(post.getId()).orElseThrow();
        assertThat(fromDb.getTitle()).isEqualTo("Updated Title");
        assertThat(fromDb.getStatus()).isEqualTo("published");
    }

    @Test
    @Transactional
    void getPost_retrievesPostById() {
        // Arrange
        BlogPost post = BlogPost.builder()
                .title("Test Post")
                .imageUrl("https://example.com/test.jpg")
                .theme("Testing")
                .status("published")
                .markdownContent("Test markdown content")
                .authorId("test-author")
                .authorName("Test Author")
                .build();
        post = blogPostRepository.save(post);

        // Act
        BlogPostDTO retrieved = blogPostService.getPost(post.getId());

        // Assert
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getId()).isEqualTo(post.getId());
        assertThat(retrieved.getTitle()).isEqualTo("Test Post");
        assertThat(retrieved.getAuthorName()).isEqualTo("Test Author");
    }

    @Test
    @Transactional
    void getPost_throwsExceptionWhenNotFound() {
        // Act & Assert
        assertThatThrownBy(() -> blogPostService.getPost(99999))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Post not found with id: 99999");
    }

    @Test
    @Transactional
    void getAllPosts_returnsAllPostsOrderedByDate() {
        // Arrange - create multiple posts
        blogPostRepository.save(BlogPost.builder()
                .title("Post 1")
                .status("draft")
                .markdownContent("Content 1")
                .authorId("author-1")
                .authorName("Author One")
                .build());

        blogPostRepository.save(BlogPost.builder()
                .title("Post 2")
                .status("published")
                .markdownContent("Content 2")
                .authorId("author-2")
                .authorName("Author Two")
                .build());

        blogPostRepository.save(BlogPost.builder()
                .title("Post 3")
                .status("draft")
                .markdownContent("Content 3")
                .authorId("author-1")
                .authorName("Author One")
                .build());

        // Act
        List<BlogPostDTO> allPosts = blogPostService.getAllPosts();

        // Assert
        assertThat(allPosts).hasSize(3);
        assertThat(allPosts).extracting("title")
                .containsExactly("Post 3", "Post 2", "Post 1"); // ordered by created_at desc
    }

    @Test
    @Transactional
    void getPublishedPosts_returnsOnlyPublishedPosts() {
        // Arrange
        blogPostRepository.save(BlogPost.builder()
                .title("Draft Post")
                .status("draft")
                .markdownContent("Draft content")
                .authorId("author-1")
                .authorName("Author One")
                .build());

        blogPostRepository.save(BlogPost.builder()
                .title("Published Post 1")
                .status("published")
                .markdownContent("Published content 1")
                .authorId("author-2")
                .authorName("Author Two")
                .build());

        blogPostRepository.save(BlogPost.builder()
                .title("Published Post 2")
                .status("published")
                .markdownContent("Published content 2")
                .authorId("author-1")
                .authorName("Author One")
                .build());

        // Act
        List<BlogPostDTO> publishedPosts = blogPostService.getPublishedPosts();

        // Assert
        assertThat(publishedPosts).hasSize(2);
        assertThat(publishedPosts).extracting("status")
                .allMatch(status -> status.equals("published"));
        assertThat(publishedPosts).extracting("title")
                .containsExactlyInAnyOrder("Published Post 1", "Published Post 2");
    }

    @Test
    @Transactional
    void deletePost_removesPostFromDatabase() {
        // Arrange
        BlogPost post = BlogPost.builder()
                .title("Post to Delete")
                .status("draft")
                .markdownContent("Will be deleted")
                .authorId("author-1")
                .authorName("Author One")
                .build();
        post = blogPostRepository.save(post);
        Integer postId = post.getId();

        // Act
        blogPostService.deletePost(postId);

        // Assert
        assertThat(blogPostRepository.findById(postId)).isEmpty();
    }

    @Test
    @Transactional
    void deletePost_throwsExceptionWhenNotFound() {
        // Act & Assert
        assertThatThrownBy(() -> blogPostService.deletePost(99999))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Post not found with id: 99999");
    }

    @Test
    @Transactional
    void updatePost_partialUpdate_onlyUpdatesProvidedFields() {
        // Arrange
        BlogPost post = BlogPost.builder()
                .title("Original Title")
                .imageUrl("https://example.com/original.jpg")
                .theme("Original Theme")
                .status("draft")
                .markdownContent("Original markdown")
                .authorId("author-1")
                .authorName("Author One")
                .build();
        post = blogPostRepository.save(post);

        // Update only the title
        UpdateBlogPostRequest partialUpdate = UpdateBlogPostRequest.builder()
                .title("New Title Only")
                .build();

        // Act
        BlogPostDTO updated = blogPostService.updatePost(post.getId(), partialUpdate);

        // Assert - only title should change
        assertThat(updated.getTitle()).isEqualTo("New Title Only");
        assertThat(updated.getImageUrl()).isEqualTo("https://example.com/original.jpg");
        assertThat(updated.getTheme()).isEqualTo("Original Theme");
        assertThat(updated.getStatus()).isEqualTo("draft");
        assertThat(updated.getMarkdownContent()).isEqualTo("Original markdown");
    }
}
