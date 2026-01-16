package com.shophub.controller;

import com.shophub.model.Notification;
import com.shophub.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"}, allowCredentials = "true")
public class NotificationController {
    
    private final NotificationRepository notificationRepository;
    
    /**
     * Get all notifications for the authenticated user
     */
    @GetMapping
    public ResponseEntity<List<Notification>> getUserNotifications(Authentication authentication) {
        String userId = authentication.getName();
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * Mark a notification as read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Integer id,
            Authentication authentication) {
        String userId = authentication.getName();
        
        notificationRepository.findById(id).ifPresent(notification -> {
            if (notification.getUserId().equals(userId)) {
                notification.setRead(true);
                notificationRepository.save(notification);
            }
        });
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Get unread notification count
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadCount(Authentication authentication) {
        String userId = authentication.getName();
        Long count = notificationRepository.countByUserIdAndRead(userId, false);
        return ResponseEntity.ok(count);
    }

    /**
     * DEBUG: Get ALL notifications in database
     */
    @GetMapping("/debug/all")
    public ResponseEntity<List<Notification>> getAllNotifications() {
        return ResponseEntity.ok(notificationRepository.findAll());
    }

    /**
     * DEBUG: Create test notification for current user
     */
    @PostMapping("/debug/test")
    public ResponseEntity<String> createTestNotification(Authentication authentication) {
        String userId = authentication.getName();
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle("TEST NOTIFICATION");
        notification.setMessage("This is a test notification for user: " + userId);
        notification.setType("test");
        notification.setRead(false);
        notification.setCreatedAt(java.time.LocalDateTime.now());
        notificationRepository.save(notification);
        return ResponseEntity.ok("Test notification created for user: " + userId);
    }
}
