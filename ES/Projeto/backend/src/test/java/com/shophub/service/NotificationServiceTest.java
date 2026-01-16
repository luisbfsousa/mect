package com.shophub.service;

import com.shophub.model.Notification;
import com.shophub.model.Order;
import com.shophub.model.User;
import com.shophub.repository.NotificationRepository;
import com.shophub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void sendOrderStatusNotification_shouldSaveNotification() {
        Order order = new Order();
        order.setOrderId(1);
        order.setUserId("user1");
        order.setOrderStatus("pending");
        order.setTrackingNumber("TRK-123");
        order.setEstimatedDeliveryDate(LocalDate.now().plusDays(3));
        order.setTotalAmount(new BigDecimal("100.00"));

        User user = new User();
        user.setUserId("user1");
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");

        when(userRepository.findById("user1")).thenReturn(Optional.of(user));
        doAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            assertEquals("user1", notification.getUserId());
            assertEquals("Order Status Updated", notification.getTitle());
            return null;
        }).when(notificationRepository).save(any(Notification.class));

        notificationService.sendOrderStatusNotification(order, "pending", "shipped");
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void sendPaymentConfirmationNotification_shouldSaveNotification() {
        Order order = new Order();
        order.setOrderId(2);
        order.setUserId("user2");
        order.setOrderStatus("processing");
        order.setTrackingNumber("TRK-456");
        order.setTotalAmount(new BigDecimal("200.00"));

        User user = new User();
        user.setUserId("user2");
        user.setEmail("pay@example.com");
        user.setFirstName("Pay");
        user.setLastName("User");

        when(userRepository.findById("user2")).thenReturn(Optional.of(user));
        doAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            assertEquals("user2", notification.getUserId());
            assertEquals("Payment Confirmed", notification.getTitle());
            return null;
        }).when(notificationRepository).save(any(Notification.class));

        notificationService.sendPaymentConfirmationNotification(order);
        verify(notificationRepository).save(any(Notification.class));
    }
}
