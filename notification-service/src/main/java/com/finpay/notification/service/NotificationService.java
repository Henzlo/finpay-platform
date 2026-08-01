package com.finpay.notification.service;

import com.finpay.notification.dto.NotificationResponse;
import com.finpay.notification.dto.NotificationSummary;
import com.finpay.notification.entity.Notification;
import com.finpay.notification.entity.NotificationChannel;
import com.finpay.notification.entity.NotificationStatus;
import com.finpay.notification.entity.NotificationType;
import com.finpay.notification.exception.NotificationNotFoundException;
import com.finpay.notification.exception.UnauthorizedException;
import com.finpay.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    public Notification createAndSendNotification(
            String userId,
            String userEmail,
            String title,
            String message,
            NotificationType type,
            String relatedEntityId,
            String actionUrl) {

        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .channel(NotificationChannel.IN_APP)
                .status(NotificationStatus.PENDING)
                .isRead(false)
                .relatedEntityId(relatedEntityId)
                .actionUrl(actionUrl)
                .createdAt(LocalDateTime.now())
                .build();

        notification = notificationRepository.save(notification);

        boolean emailSent = emailService.sendEmail(userEmail, title, message);
        if (emailSent) {
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        } else {
            notification.setStatus(NotificationStatus.FAILED);
            log.warn("Email delivery failed for notificationId={} userId={}", notification.getId(), userId);
        }

        return notificationRepository.save(notification);
    }

    public List<NotificationResponse> getMyNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public NotificationResponse markAsRead(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found: " + notificationId));

        if (!userId.equals(notification.getUserId())) {
            throw new UnauthorizedException("You cannot modify this notification");
        }

        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        if (notification.getStatus() != NotificationStatus.FAILED) {
            notification.setStatus(NotificationStatus.READ);
        }
        return toResponse(notificationRepository.save(notification));
    }

    public void markAllAsRead(String userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalse(userId);
        LocalDateTime now = LocalDateTime.now();
        unread.forEach(notification -> {
            notification.setRead(true);
            notification.setReadAt(now);
            if (notification.getStatus() != NotificationStatus.FAILED) {
                notification.setStatus(NotificationStatus.READ);
            }
        });
        notificationRepository.saveAll(unread);
    }

    public NotificationSummary getNotificationSummary(String userId) {
        List<NotificationResponse> all = getMyNotifications(userId);
        List<NotificationResponse> recent = all.stream().limit(10).toList();
        long unread = getUnreadCount(userId);
        return NotificationSummary.builder()
                .totalCount(all.size())
                .unreadCount(unread)
                .recent(recent)
                .build();
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .channel(notification.getChannel())
                .status(notification.getStatus())
                .isRead(notification.isRead())
                .actionUrl(notification.getActionUrl())
                .relatedEntityId(notification.getRelatedEntityId())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
