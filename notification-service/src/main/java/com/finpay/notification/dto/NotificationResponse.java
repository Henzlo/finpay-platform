package com.finpay.notification.dto;

import com.finpay.notification.entity.NotificationChannel;
import com.finpay.notification.entity.NotificationStatus;
import com.finpay.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private String id;
    private String userId;
    private String title;
    private String message;
    private NotificationType type;
    private NotificationChannel channel;
    private NotificationStatus status;
    private boolean isRead;
    private String actionUrl;
    private String relatedEntityId;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
