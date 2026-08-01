package com.finpay.notification.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    private String id;

    private String userId;

    private String title;

    private String message;

    private NotificationType type;

    private NotificationChannel channel;

    private NotificationStatus status;

    @Builder.Default
    private boolean isRead = false;

    private String actionUrl;

    private String relatedEntityId;

    private LocalDateTime createdAt;

    private LocalDateTime readAt;

    private LocalDateTime sentAt;
}
