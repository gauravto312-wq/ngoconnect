package com.ngoconnect.service;

import com.ngoconnect.dto.NotificationResponse;
import com.ngoconnect.entity.Notification;
import java.util.List;

public interface NotificationService {
    Notification sendNotification(Long userId, String title, String message, String type, String entityType, Long entityId);
    List<NotificationResponse> getUserNotifications(Long userId);
    List<NotificationResponse> getUnreadNotifications(Long userId);
    void markAsRead(Long notificationId, Long userId);
    void markAllAsRead(Long userId);
    Long getUnreadCount(Long userId);
}
