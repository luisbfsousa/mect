package com.shophub.repository;

import com.shophub.model.BlogPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, Integer> {
    
    // Get all posts ordered by most recent
    List<BlogPost> findAllByOrderByCreatedAtDesc();
    
    // Get published posts only (for public pages)
    List<BlogPost> findByStatusOrderByCreatedAtDesc(String status);
    
    // Get posts by author
    List<BlogPost> findByAuthorIdOrderByCreatedAtDesc(String authorId);
}