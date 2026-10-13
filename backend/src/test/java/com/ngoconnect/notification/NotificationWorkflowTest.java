package com.ngoconnect.notification;

import com.ngoconnect.dto.AuthResponse;
import com.ngoconnect.dto.NotificationResponse;
import com.ngoconnect.dto.RegisterRequest;
import com.ngoconnect.entity.Notification;
import com.ngoconnect.exception.ForbiddenException;
import com.ngoconnect.service.AuthService;
import com.ngoconnect.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = com.ngoconnect.NGOConnectApplication.class)
@ActiveProfiles("test")
@Transactional
public class NotificationWorkflowTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private NotificationService notificationService;

    private AuthResponse user1Auth;
    private AuthResponse user2Auth;

    @BeforeEach
    void setUp() {
        RegisterRequest user1Req = new RegisterRequest();
        user1Req.setName("User One");
        user1Req.setEmail("user1.notif@ngoconnect.org");
        user1Req.setPhone("9876543201");
        user1Req.setPassword("Password123!");
        user1Req.setRole("CITIZEN");
        user1Auth = authService.register(user1Req);

        RegisterRequest user2Req = new RegisterRequest();
        user2Req.setName("User Two");
        user2Req.setEmail("user2.notif@ngoconnect.org");
        user2Req.setPhone("9876543202");
        user2Req.setPassword("Password123!");
        user2Req.setRole("CITIZEN");
        user2Auth = authService.register(user2Req);
    }

    @Test
    @DisplayName("Should accurately calculate and decrement unread notifications count step-by-step")
    void testAccurateUnreadCountAndStepByStepDecrement() {
        // Initially 0
        assertEquals(0L, notificationService.getUnreadCount(user1Auth.getId()));

        // Add 3 notifications for User 1
        Notification n1 = notificationService.sendNotification(user1Auth.getId(), "Title 1", "Msg 1", "SYSTEM", "NONE", null);
        Notification n2 = notificationService.sendNotification(user1Auth.getId(), "Title 2", "Msg 2", "SYSTEM", "NONE", null);
        Notification n3 = notificationService.sendNotification(user1Auth.getId(), "Title 3", "Msg 3", "SYSTEM", "NONE", null);

        // Count should be 3
        assertEquals(3L, notificationService.getUnreadCount(user1Auth.getId()));
        List<NotificationResponse> unreadList = notificationService.getUnreadNotifications(user1Auth.getId());
        assertEquals(3, unreadList.size());

        // Mark 1 as read -> count should be 2
        notificationService.markAsRead(n1.getId(), user1Auth.getId());
        assertEquals(2L, notificationService.getUnreadCount(user1Auth.getId()));

        // Mark 2nd as read -> count should be 1
        notificationService.markAsRead(n2.getId(), user1Auth.getId());
        assertEquals(1L, notificationService.getUnreadCount(user1Auth.getId()));

        // Mark 3rd as read -> count should be 0
        notificationService.markAsRead(n3.getId(), user1Auth.getId());
        assertEquals(0L, notificationService.getUnreadCount(user1Auth.getId()));
        assertEquals(0, notificationService.getUnreadNotifications(user1Auth.getId()).size());
    }

    @Test
    @DisplayName("Should mark all notifications as read and reset unread count to 0")
    void testMarkAllAsRead() {
        notificationService.sendNotification(user1Auth.getId(), "Alert A", "Message A", "SYSTEM", "NONE", null);
        notificationService.sendNotification(user1Auth.getId(), "Alert B", "Message B", "SYSTEM", "NONE", null);
        notificationService.sendNotification(user1Auth.getId(), "Alert C", "Message C", "SYSTEM", "NONE", null);

        assertEquals(3L, notificationService.getUnreadCount(user1Auth.getId()));

        notificationService.markAllAsRead(user1Auth.getId());

        assertEquals(0L, notificationService.getUnreadCount(user1Auth.getId()));
        assertEquals(0, notificationService.getUnreadNotifications(user1Auth.getId()).size());

        List<NotificationResponse> all = notificationService.getUserNotifications(user1Auth.getId());
        assertEquals(3, all.size());
        assertTrue(all.stream().allMatch(NotificationResponse::getIsRead));
    }

    @Test
    @DisplayName("Should reject marking another user's notification with ForbiddenException")
    void testForbiddenWhenMarkingAnotherUsersNotification() {
        Notification user1Notification = notificationService.sendNotification(user1Auth.getId(), "Private Note", "Sensitive", "SYSTEM", "NONE", null);

        // User 2 attempts to mark User 1's notification as read
        assertThrows(ForbiddenException.class, () -> {
            notificationService.markAsRead(user1Notification.getId(), user2Auth.getId());
        });

        // User 1's unread count must still be 1
        assertEquals(1L, notificationService.getUnreadCount(user1Auth.getId()));
        assertFalse(notificationService.getUnreadNotifications(user1Auth.getId()).get(0).getIsRead());
    }

    @Test
    @DisplayName("Should prevent cross-user unread count leakage")
    void testPreventCrossUserCountLeakage() {
        notificationService.sendNotification(user1Auth.getId(), "User 1 Note 1", "Msg 1", "SYSTEM", "NONE", null);
        notificationService.sendNotification(user1Auth.getId(), "User 1 Note 2", "Msg 2", "SYSTEM", "NONE", null);

        notificationService.sendNotification(user2Auth.getId(), "User 2 Note 1", "Msg 1", "SYSTEM", "NONE", null);

        assertEquals(2L, notificationService.getUnreadCount(user1Auth.getId()));
        assertEquals(1L, notificationService.getUnreadCount(user2Auth.getId()));

        // User 1 marks all as read
        notificationService.markAllAsRead(user1Auth.getId());

        assertEquals(0L, notificationService.getUnreadCount(user1Auth.getId()));
        // User 2 unread count remains untouched
        assertEquals(1L, notificationService.getUnreadCount(user2Auth.getId()));
    }
}
