package com.shophub.repository;

import com.shophub.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
    
    Long countByUserIdAndRead(String userId, Boolean read);
}
