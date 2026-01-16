package com.shophub.controller;

import com.shophub.model.Notification;
import com.shophub.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationController notificationController;

    @Mock
    private org.springframework.security.core.Authentication authentication;

    @Test
    void getUserNotifications_shouldReturnNotifications() {
        List<Notification> notifications = Arrays.asList(new Notification(), new Notification());
        when(authentication.getName()).thenReturn("user1");
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("user1")).thenReturn(notifications);

        ResponseEntity<List<Notification>> response = notificationController.getUserNotifications(authentication);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(2, response.getBody().size());
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc("user1");
    }

    @Test
    void markAsRead_shouldUpdateNotificationIfOwnedByUser() {
        Notification notification = new Notification();
        notification.setId(1);
        notification.setUserId("user1");
        notification.setRead(false);
        when(authentication.getName()).thenReturn("user1");
        when(notificationRepository.findById(1)).thenReturn(java.util.Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        ResponseEntity<Void> response = notificationController.markAsRead(1, authentication);
        assertEquals(200, response.getStatusCodeValue());
    assertTrue(notification.getRead());
        verify(notificationRepository).findById(1);
        verify(notificationRepository).save(notification);
    }
}
